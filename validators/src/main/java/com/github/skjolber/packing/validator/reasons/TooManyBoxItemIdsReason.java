package com.github.skjolber.packing.validator.reasons;

/**
 * 
 * There is more box item types than expected.
 * 
 */


public class TooManyBoxItemIdsReason extends AbstractValidatorResultReason {

	private static final long serialVersionUID = 1L;
	
	private static final int CODE = 2;

	public TooManyBoxItemIdsReason(String message) {
		super(CODE, message);
	}
	
}

