package com.jbrisbin.async.http;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;

import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.memory.ByteBufferWrapper;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class Request {

	static final int BUFFER_SIZE = 8192;

	public enum Method {
		GET, PUT, POST, DELETE, HEAD
	}

	protected URL url;
	protected AsyncHttpClient client;
	protected Method method;
	protected String contentType = "application/octet-stream";
	protected Long contentLength = 0L;
	protected Map<String, String> headers = new HashMap<String, String>();
	protected ByteBuffer body;
	protected ReadableByteChannel readableByteChannel;
	protected Response response;
	protected HttpRequestPacket request = null;
	protected boolean complete = false;
	protected boolean chunked = false;

	public Request(URL url, Method method) {
		this.url = url;
		this.client = new AsyncHttpClient(url.getHost(), url.getPort());
		this.method = method;
		this.response = new Response(this);
	}

	public Request(URL url, Method method, Map<String, String> headers) {
		this.url = url;
		this.client = new AsyncHttpClient(url.getHost(), url.getPort());
		this.method = method;
		setHeaders(headers);
		this.response = new Response(this);
	}

	public Request(URL url, Method method, Map<String, String> headers, ByteBuffer body, String contentType) {
		this.url = url;
		this.client = new AsyncHttpClient(url.getHost(), url.getPort());
		this.method = method;
		setHeaders(headers);
		this.body = body;
		this.contentType = contentType;
		contentLength = new Long(body.remaining());
		this.response = new Response(this);
	}

	public URL getUrl() {
		return url;
	}

	public AsyncHttpClient getClient() {
		return client;
	}

	public Method getMethod() {
		return method;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public Request setHeaders(Map<String, String> headers) {
		if (null != headers)
			this.headers.putAll(headers);
		return this;
	}

	public String getContentType() {
		return contentType;
	}

	public Request setContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public Long getContentLength() {
		return (null != contentLength ? contentLength : (null != body ? body.remaining() : -1));
	}

	public Request setContentLength(Long contentLength) {
		this.contentLength = contentLength;
		return this;
	}

	public ByteBuffer getBody() {
		return body;
	}

	public Request setBody(ByteBuffer body) {
		this.body = body;
		return this;
	}

	public ReadableByteChannel getReadableByteChannel() {
		if (null == readableByteChannel && null != body) {
			return new BufferReadableByteChannel(new ByteBufferWrapper(body));
		} else {
			return readableByteChannel;
		}
	}

	public Request setReadableByteChannel(ReadableByteChannel readableByteChannel) {
		this.readableByteChannel = readableByteChannel;
		return this;
	}

	public Response getResponse() {
		return response;
	}

	HttpRequestPacket getRequest() {
		return request;
	}

	Request setRequest(HttpRequestPacket request) {
		this.request = request;
		return this;
	}

	Request setComplete(boolean complete) {
		this.complete = complete;
		return this;
	}

	boolean isComplete() {
		return this.complete;
	}

	public boolean isChunked() {
		return chunked;
	}

	public Request setChunked(boolean chunked) {
		this.chunked = chunked;
		return this;
	}

	public Response send() throws IOException {
		return client.send(this);
	}

	public WritableByteChannel start() throws IOException {
		return client.start(this);
	}

	public Response complete() throws IOException {
		return client.complete(this);
	}

	@Override public String toString() {
		return "Request {" +
				"\r\n\turl=" + url +
				", \r\n\tclient=" + client +
				", \r\n\tmethod=" + method +
				", \r\n\tcontentType='" + contentType + '\'' +
				", \r\n\tcontentLength=" + contentLength +
				", \r\n\theaders=" + headers +
				", \r\n\tbody=" + body +
				", \r\n\treadableByteChannel=" + readableByteChannel +
				", \r\n\tresponse=" + response +
				", \r\n\trequest=" + request +
				", \r\n\tcomplete=" + complete +
				", \r\n\tchunked=" + chunked +
				"\r\n}";
	}

}
