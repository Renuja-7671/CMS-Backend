package com.epic.cms.filter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * Request wrapper that replaces the request body with decrypted content.
 * Allows the filter to decrypt the payload and make it available to controllers.
 */
public class DecryptedRequestWrapper extends HttpServletRequestWrapper {

	private final String decryptedBody;
	private final byte[] decryptedBodyBytes;

	public DecryptedRequestWrapper(HttpServletRequest request, String decryptedBody) {
		super(request);
		this.decryptedBody = decryptedBody;
		this.decryptedBodyBytes = decryptedBody.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decryptedBodyBytes);
		
		return new ServletInputStream() {
			@Override
			public int read() throws IOException {
				return byteArrayInputStream.read();
			}

			@Override
			public boolean isFinished() {
				return byteArrayInputStream.available() == 0;
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setReadListener(ReadListener readListener) {
				throw new UnsupportedOperationException("ReadListener not supported");
			}
		};
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
	}

	@Override
	public int getContentLength() {
		return decryptedBodyBytes.length;
	}

	@Override
	public long getContentLengthLong() {
		return decryptedBodyBytes.length;
	}

	/**
	 * Get the decrypted body as a string.
	 */
	public String getDecryptedBody() {
		return decryptedBody;
	}
}
