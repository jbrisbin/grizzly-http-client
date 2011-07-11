package com.jbrisbin.async.http.test

import com.jbrisbin.async.http.HttpException
import com.jbrisbin.async.http.Request
import com.jbrisbin.async.http.Response
import com.jbrisbin.async.http.ResponseCallback
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.WritableByteChannel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification
import static com.jbrisbin.async.http.AsyncHttpClient.*

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
class AsyncHttpClientSpec extends Specification {

	Logger log = LoggerFactory.getLogger(getClass())
	CountDownLatch latch

	def setup() {
		latch = new CountDownLatch(1)
	}

	def cleanup() {
	}

	def await(Future f) {
		f.get(30, TimeUnit.SECONDS)
		f
	}

	def "Test connect asynchronously"() {

		when:
		Request req = GET("http://localhost:8098/riak/status")
		Response resp = req.send()
		ByteBuffer content = resp.get(30, TimeUnit.SECONDS)
		resp.addCallback(new ResponseCallback() {
			@Override boolean isWatch() {
				true
			}

			@Override void success(Map<String, String> headers, ByteBuffer body) {
				latch.countDown()
			}

			@Override void failure(Map<String, String> headers, Throwable t) {
				latch.countDown()
				log.error(t.message, t)
			}
		})
		latch.await(30, TimeUnit.SECONDS)
		log.info "Headers: ${resp.headers}"
		log.info "Content: ${new String(content.array())}"

		then:
		null != content
		content.remaining() > 0

	}

	def "Test PUT data"() {

		given:
		def bytes = "Hello World!".bytes

		when:
		def req = PUT("http://localhost:8098/riak/upload/text")
		req.contentType = "text/plain"
		req.contentLength = bytes.length
		def out = req.start()
		out.write(ByteBuffer.wrap("Hello ".bytes))
		out.write(ByteBuffer.wrap("World!".bytes))
		def resp = await req.complete()

		then:
		null != resp

	}

	def "Test DELETE data"() {

		when:
		Response delResp = await DELETE("http://localhost:8098/riak/upload/text?rw=all").send()
		Response getResp = await GET("http://localhost:8098/riak/upload/text").send()

		then:
		null != delResp
		def e = thrown(ExecutionException)
		e.cause?.class == HttpException

	}

	def "Test upload file"() {

		given:
		FileChannel input = new RandomAccessFile("src/test/files/lorem_ipsum.txt", "r").channel

		when:
		Request req = PUT("http://localhost:8098/riak/upload/text")
		req.contentType = "text/plain"
		req.contentLength = input.size()
		WritableByteChannel w = req.start()
		input.transferTo(0, input.size(), w)
		Response resp = await req.complete()

		then:
		resp.status == 204

	}

}
