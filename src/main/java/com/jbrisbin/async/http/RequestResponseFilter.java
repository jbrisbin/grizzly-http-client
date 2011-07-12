package com.jbrisbin.async.http;

import java.io.IOException;
import java.util.Map;
import java.util.Stack;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.memory.ByteBufferWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class RequestResponseFilter extends BaseFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestResponseFilter.class);
	private static final Attribute<Request> PENDING_REQUEST = Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute("pending-request");

	private Stack<Request> pendingRequests = new Stack<Request>();

	@Override public NextAction handleRead(FilterChainContext ctx) throws IOException {
		HttpContent content = ctx.getMessage();
		HttpHeader header = content.getHttpHeader();
		HttpStatus status = ((HttpResponsePacket) header).getHttpStatus();
		int statusCode = status.getStatusCode();

		Request request = PENDING_REQUEST.get(ctx.getAttributes());
		if (null == request) {
			synchronized (pendingRequests) {
				if (!pendingRequests.empty()) {
					request = pendingRequests.peek();
					PENDING_REQUEST.set(ctx.getAttributes(), request);
				} else {
					log.error("Response from server without matching request: " + content);
					throw new IllegalStateException(new String(status.getReasonPhraseBytes()));
				}
			}
		}
		Response response = request.getResponse();
		response.setStatus(statusCode);
		Map<String, String> respHeaders = response.getHeaders();
		for (String name : header.getHeaders().names()) {
			String val = header.getHeader(name);
			respHeaders.put(name, val);
		}

		if (null != content.getContent() && statusCode >= 200 && statusCode < 300) {
			response.addContent(content.getContent().toByteBuffer(), content.isLast());
		} else {
			switch (statusCode) {
				case 400:
				case 403:
				case 404:
				case 500:
					response.getFuture().failure(new HttpException(new String(status.getReasonPhraseBytes()), statusCode));
					break;
			}
		}

		if (content.isLast()) {
			pendingRequests.pop();
			PENDING_REQUEST.remove(ctx.getAttributes());
			return ctx.getStopAction();
		} else {
			return ctx.getStopAction(content);
		}
	}

	@SuppressWarnings({"unchecked"})
	@Override public NextAction handleWrite(FilterChainContext ctx) throws IOException {
		Object o = ctx.getMessage();
		if (o instanceof Request) {
			Request request = (Request) o;
			if (null != request) {
				if (null == request.getRequest()) {
					// Start this request by sending the headers
					String urlPath = request.getUrl().getPath();
					if (null != request.getUrl().getQuery()) {
						urlPath += "?" + request.getUrl().getQuery();
					}
					HttpRequestPacket.Builder rb = HttpRequestPacket.builder()
							.protocol(Protocol.HTTP_1_1)
							.uri(urlPath);
					// Method
					switch (request.getMethod()) {
						case HEAD:
							rb.method(Method.HEAD);
							request.setComplete(true);
							break;
						case GET:
							rb.method(Method.GET);
							request.setComplete(true);
							break;
						case PUT:
							rb.method(Method.PUT);
							break;
						case POST:
							rb.method(Method.POST);
							break;
						case DELETE:
							rb.method(Method.DELETE);
							request.setComplete(true);
							break;
					}
					// Custom headers
					Map<String, String> headers = request.getHeaders();
					for (Map.Entry<String, String> hentry : headers.entrySet()) {
						rb.header(hentry.getKey(), hentry.getValue());
					}
					// Content-Type
					if (null != request.getContentType()) {
						rb.contentType(request.getContentType());
					}
					// Content-Length
					if (request.isChunked()) {
						headers.put("Transfer-Encoding", "chunked");
					} else {
						rb.contentLength(request.getContentLength());
					}

					// Write request
					HttpRequestPacket requestp = rb.build();
					if (log.isDebugEnabled())
						log.debug("Writing " + request.getMethod() + " " + urlPath);
					pendingRequests.push(request);
					ctx.write(requestp);

					request.setRequest(requestp);
				}

				// Body
				if (null != request.getBody()) {
					HttpContent hc = request.getRequest().httpContentBuilder()
							.content(new ByteBufferWrapper(request.getBody()))
							.build();
					if (log.isDebugEnabled())
						log.debug("Writing content chunk: " + hc);
					ctx.write(hc);
					request.setBody(null);
				}

				if (request.isComplete()) {
					if (log.isDebugEnabled())
						log.debug("Finishing request: " + request);
					ctx.write(request.getRequest().httpTrailerBuilder().build());
				}
				ctx.setMessage(null);
			}

			return ctx.getStopAction();
		} else {
			return ctx.getInvokeAction();
		}
	}

}
