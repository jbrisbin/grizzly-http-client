package com.jbrisbin.async.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class Response implements Future<ByteBuffer> {

	private final Request request;
	private final FutureImpl<ByteBuffer> future;
	private ConcurrentLinkedQueue<ResponseCallback> callbacks = new ConcurrentLinkedQueue<ResponseCallback>();
	private WritableByteChannel writableByteChannel = null;
	private int status;
	private Map<String, String> headers = new HashMap<String, String>();
	private String contentType;
	private Long contentLength = 0L;
	private List<ByteBuffer> body = new ArrayList<ByteBuffer>();
	private final String writeMutex = "write";

	Response(Request request) {
		this.request = request;
		future = new SafeFutureImpl<ByteBuffer>() {
			@Override public void failure(Throwable failure) {
				super.failure(failure);
				for (ResponseCallback callback : callbacks) {
					callback.failure(headers, failure);
				}
			}

			@Override protected void done(int lifeCounter) {
				super.done(lifeCounter);
				List<ResponseCallback> toRemove = new ArrayList<ResponseCallback>();
				for (ResponseCallback callback : callbacks) {
					callback.success(headers, getResult());
					if (callback.isWatch()) {
						toRemove.add(callback);
					}
				}
				for (ResponseCallback callback : toRemove) {
					callbacks.remove(callback);
				}
			}
		};
	}

	public Request getRequest() {
		return request;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers.putAll(headers);
	}

	public void setHeaders(Map<String, String> headers, boolean overwrite) {
		if (overwrite) {
			this.headers = headers;
		} else {
			this.headers.putAll(headers);
		}
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Long getContentLength() {
		return contentLength;
	}

	public byte[] getContent() {
		if (future.isDone() && null != future.getResult())
			return future.getResult().array();
		else
			return null;
	}

	void addContent(ByteBuffer buffer, boolean isLast) throws IOException {
		synchronized (writeMutex) {
			if (null != writableByteChannel) {
				contentLength += writableByteChannel.write(buffer);
			} else {
				if (null != buffer) {
					body.add(buffer);
					contentLength += buffer.remaining();
				}
				if (isLast && contentLength > 0) {
					ByteBuffer fullBuffer = ByteBuffer.allocate(contentLength.intValue());
					for (ByteBuffer b : body) {
						fullBuffer.put(b);
					}
					fullBuffer.rewind();
					future.result(fullBuffer);
				} else if (isLast) {
					future.result(null);
				}
			}
		}
	}

	@Override public boolean cancel(boolean b) {
		return future.cancel(b);
	}

	@Override public boolean isCancelled() {
		return future.isCancelled();
	}

	@Override public boolean isDone() {
		return future.isDone();
	}

	@Override public ByteBuffer get() throws InterruptedException, ExecutionException {
		return future.get();
	}

	@Override
	public ByteBuffer get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
		return future.get(l, timeUnit);
	}

	public WritableByteChannel getWritableByteChannel() {
		return writableByteChannel;
	}

	public void setWritableByteChannel(WritableByteChannel channel) throws IOException {
		this.writableByteChannel = channel;
		if (null != writableByteChannel) {
			synchronized (writeMutex) {
				if (body.size() > 0) {
					for (ByteBuffer b : body) {
						writableByteChannel.write(b);
					}
				}
			}
		}
	}

	public Response addCallback(ResponseCallback callback) {
		if (future.isDone()) {
			callback.success(headers, future.getResult());
		} else {
			callbacks.add(callback);
		}
		return this;
	}

	public ConcurrentLinkedQueue<ResponseCallback> getCallbacks() {
		return callbacks;
	}

	public FutureImpl<ByteBuffer> getFuture() {
		return future;
	}

	@Override public String toString() {
		return "Response {" +
				"\r\n\tfuture=" + future +
				", \r\n\tcallbacks=" + callbacks +
				", \r\n\twritableByteChannel=" + writableByteChannel +
				", \r\n\tstatus=" + status +
				", \r\n\theaders=" + headers +
				", \r\n\tcontentType='" + contentType + '\'' +
				", \r\n\tcontentLength=" + contentLength +
				", \r\n\tbody=" + body +
				"\r\n}";
	}
}
