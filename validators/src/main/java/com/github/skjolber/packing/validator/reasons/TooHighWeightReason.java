package com.github.skjolber.packing.validator.reasons;

/**
 * Indicates that the total weight exceeds the allowed limit.
 */


public class TooHighWeightReason extends AbstractValidatorResultReason {

	private static final long serialVersionUID = 1L;
	
	private static final int CODE = 3;

	public TooHighWeightReason(String message) {
		super(CODE, message);
	}
	
}

