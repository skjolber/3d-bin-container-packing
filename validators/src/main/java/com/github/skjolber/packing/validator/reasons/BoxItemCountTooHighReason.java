package com.github.skjolber.packing.validator.reasons;

public class BoxItemCountTooHighReason extends AbstractValidatorResultReason {

	private static final long serialVersionUID = 1L;
	
	private static final int CODE = 8;
	
	public BoxItemCountTooHighReason(String message) {
		super(CODE, message);
	}
	
}
