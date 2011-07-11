package com.jbrisbin.async.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.glassfish.grizzly.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class BufferReadableByteChannel implements ReadableByteChannel {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private Buffer buffer;

	public BufferReadableByteChannel(Buffer buffer) {
		this.buffer = buffer;
	}

	public Buffer getBuffer() {
		return buffer;
	}

	public void setBuffer(Buffer buffer) {
		this.buffer = buffer;
	}

	@Override public int read(ByteBuffer byteBuffer) throws IOException {
		int start = buffer.position();
		buffer.get(byteBuffer);
		int length = buffer.position() - start;
		log.debug("Read " + length + " bytes");
		return length;
	}

	@Override public boolean isOpen() {
		return true;
	}

	@Override public void close() throws IOException {
		buffer.dispose();
	}

}
