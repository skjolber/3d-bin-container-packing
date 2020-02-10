package com.github.skjolber.packing;

public class PackagerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PackagerException() {
		super();
	}

	public PackagerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public PackagerException(String message, Throwable cause) {
		super(message, cause);
	}

	public PackagerException(String message) {
		super(message);
	}

	public PackagerException(Throwable cause) {
		super(cause);
	}
	
}
