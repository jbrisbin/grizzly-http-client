package com.jbrisbin.async.http;

import static com.jbrisbin.async.http.Request.Method.*;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.HttpClientFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class AsyncHttpClient {

	private static final Logger log = LoggerFactory.getLogger(AsyncHttpClient.class);

	private String host = "localhost";
	private Integer port = 80;
	private Long timeout = 30L;
	private TCPNIOTransport transport = null;
	private FilterChain filterChain;
	private Connection connection = null;

	AsyncHttpClient(String host, Integer port) {
		this.host = host;
		this.port = port;
	}

	public static AsyncHttpClient connect(String host, Integer port) {
		return new AsyncHttpClient(host, port);
	}

	public static Request GET(String url) throws IOException {
		return new Request(new URL(url), GET);
	}

	public static Request GET(String url, Map<String, String> headers) throws IOException {
		return new Request(new URL(url), GET, headers);
	}

	public static Request PUT(String url) throws IOException {
		return new Request(new URL(url), PUT);
	}

	public static Request PUT(String url, Map<String, String> headers) throws IOException {
		return new Request(new URL(url), PUT, headers);
	}

	public static Request POST(String url, Map<String, String> headers, byte[] content) throws IOException {
		return new Request(new URL(url), POST, headers);
	}

	public static Request DELETE(String url) throws IOException {
		return new Request(new URL(url), DELETE);
	}

	public static Request DELETE(String url, Map<String, String> headers) throws IOException {
		return new Request(new URL(url), DELETE, headers);
	}

	private void startTransport() throws IOException {
		if (null == transport) {
			TCPNIOTransportBuilder tb = TCPNIOTransportBuilder.newInstance();
			tb.setTcpNoDelay(true);
			tb.setKeepAlive(false);
			transport = tb.build();

			FilterChainBuilder fcb = FilterChainBuilder.stateless();
			fcb.add(new TransportFilter());
			fcb.add(new HttpClientFilter());
			fcb.add(new RequestResponseFilter());
			filterChain = fcb.build();

			transport.setProcessor(filterChain);
			transport.start();
		}
	}

	private Connection getConnection() throws IOException {
		if (null == connection) {
			startTransport();
			try {
				connection = transport.connect(host, port).get(timeout, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			} catch (ExecutionException e) {
				log.error(e.getMessage(), e);
			} catch (TimeoutException e) {
				log.error(e.getMessage(), e);
			}
		}
		return connection;
	}

	private int findRequestResponseFilterIndex() {
		for (int i = 0; i < filterChain.size(); i++) {
			if (filterChain.get(i) instanceof RequestResponseFilter) {
				return i;
			}
		}
		throw new IllegalStateException("No HttpClientFilter found in the filter chain! This should not happen.");
	}

	public String getHost() {
		return host;
	}

	public AsyncHttpClient setHost(String host) {
		this.host = host;
		return this;
	}

	public Integer getPort() {
		return port;
	}

	public AsyncHttpClient setPort(Integer port) {
		this.port = port;
		return this;
	}

	public Long getTimeout() {
		return timeout;
	}

	public AsyncHttpClient setTimeout(Long timeout) {
		this.timeout = timeout;
		transport.setConnectionTimeout(timeout.intValue());
		return this;
	}

	public AsyncHttpClient addUpstreamFilter(Filter filter) {
		filterChain.add(findRequestResponseFilterIndex() - 1, filter);
		return this;
	}

	public AsyncHttpClient addFilter(int idx, Filter filter) {
		filterChain.add(idx, filter);
		return this;
	}

	public AsyncHttpClient addDownstreamFilter(Filter filter) {
		filterChain.add(filter);
		return this;
	}

	public AsyncHttpClient transferTo(WritableByteChannel channel) {

		return this;
	}

	@SuppressWarnings({"unchecked"})
	public WritableByteChannel start(Request request) throws IOException {
		getConnection().write(request);
		return new RequestByteChannel(request);
	}

	@SuppressWarnings({"unchecked"})
	public Response complete(Request request) throws IOException {
		request.setComplete(true);
		getConnection().write(request);
		return request.getResponse();
	}

	@SuppressWarnings({"unchecked"})
	public Response send(Request request) throws IOException {
		getConnection().write(request);
		return request.getResponse();
	}

	@SuppressWarnings({"unchecked"})
	public List<Response> send(Request... requests) throws IOException {
		List<Response> responses = new ArrayList<Response>();
		for (Request request : requests) {
			responses.add(send(request));
		}
		return responses;
	}

	public void dispose() throws IOException {
		transport.stop();
	}

	private class RequestByteChannel implements WritableByteChannel {
		private Request request;

		private RequestByteChannel(Request request) {
			this.request = request;
		}

		@SuppressWarnings({"unchecked"})
		@Override public int write(ByteBuffer byteBuffer) throws IOException {
			int size = byteBuffer.remaining();
			request.setBody(byteBuffer);
			getConnection().write(request);
			return size;
		}

		@Override public boolean isOpen() {
			return true;
		}

		@Override public void close() throws IOException {
		}
	}

}
