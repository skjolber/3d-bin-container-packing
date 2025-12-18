package com.github.skjolber.packing.validator.reasons;

import com.github.skjolber.packing.api.validator.ValidatorResultReason;

public abstract class AbstractValidatorResultReason implements ValidatorResultReason {

	protected final int code;
	protected final String message;
	
	public AbstractValidatorResultReason(int code, String message) {
		super();
		this.code = code;
		this.message = message;
	}
	
	public int getCode() {
		return code;
	}
	
	public String getMessage() {
		return message;
	}
	
}
