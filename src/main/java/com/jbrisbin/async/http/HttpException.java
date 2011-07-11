package com.jbrisbin.async.http;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class HttpException extends Exception {

	private int status;

	public HttpException(int status) {
		this.status = status;
	}

	public HttpException(String s, int status) {
		super(s);
		this.status = status;
	}

	public HttpException(String s, Throwable throwable, int status) {
		super(s, throwable);
		this.status = status;
	}

	public HttpException(Throwable throwable, int status) {
		super(throwable);
		this.status = status;
	}

}
