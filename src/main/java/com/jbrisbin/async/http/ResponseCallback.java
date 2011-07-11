package com.jbrisbin.async.http;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public interface ResponseCallback {

	boolean isWatch();

	void success(Map<String, String> headers, ByteBuffer body);

	void failure(Map<String, String> headers, Throwable t);

}
