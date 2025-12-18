package com.github.skjolber.packing.validator.reasons;

/**
 * 
 * There is more box item types than expected.
 * 
 */


public class TooHighWeightReason extends AbstractValidatorResultReason {

	private static final long serialVersionUID = 1L;
	
	private static final int CODE = 3;

	public TooHighWeightReason(String message) {
		super(CODE, message);
	}
	
}

