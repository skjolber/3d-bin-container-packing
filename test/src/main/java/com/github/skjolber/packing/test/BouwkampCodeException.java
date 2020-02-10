package com.github.skjolber.packing.test;

public class BouwkampCodeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BouwkampCodeException() {
		super();
	}

	public BouwkampCodeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public BouwkampCodeException(String message, Throwable cause) {
		super(message, cause);
	}

	public BouwkampCodeException(String message) {
		super(message);
	}

	public BouwkampCodeException(Throwable cause) {
		super(cause);
	}
	
}
