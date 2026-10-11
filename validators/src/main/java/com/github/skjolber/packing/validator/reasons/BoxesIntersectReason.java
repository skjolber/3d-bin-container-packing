package com.github.skjolber.packing.validator.reasons;

public class BoxesIntersectReason extends AbstractValidatorResultReason {

	private static final long serialVersionUID = 1L;
	
	private static final int CODE = 10;
	
	public BoxesIntersectReason(String message) {
		super(CODE, message);
	}
	
}
