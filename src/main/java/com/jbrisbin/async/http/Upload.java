package com.jbrisbin.async.http;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class Upload extends Request {

	private File upload;

	public Upload(URL url, Method method, File upload) {
		super(url, method);
		this.upload = upload;
		if (method != Method.PUT || method != Method.POST) {
			throw new IllegalArgumentException("Uploads have to use PUT or POST");
		}
	}

	public Upload(URL url, Method method, Map<String, String> headers, File upload) {
		super(url, method, headers);
		this.upload = upload;
		if (method != Method.PUT || method != Method.POST) {
			throw new IllegalArgumentException("Uploads have to use PUT or POST");
		}
	}

	public Upload(URL url, Method method, Map<String, String> headers, ByteBuffer body, String contentType, File upload) {
		super(url, method, headers, body, contentType);
		this.upload = upload;
		if (method != Method.PUT || method != Method.POST) {
			throw new IllegalArgumentException("Uploads have to use PUT or POST");
		}
	}

}
