package com.github.skjolber.packing.validator.reasons;

public class BoxItemCountTooLowReason extends AbstractValidatorResultReason {

	private static final long serialVersionUID = 1L;
	
	private static final int CODE = 7;

	public BoxItemCountTooLowReason(String message) {
		super(CODE, message);
	}
	
}
