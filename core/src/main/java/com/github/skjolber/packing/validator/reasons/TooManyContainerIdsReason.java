package com.github.skjolber.packing.validator.reasons;

/**
 * 
 * There is more box item types than expected.
 * 
 */


public class TooManyContainerIdsReason extends AbstractValidatorResultReason {

	private static final long serialVersionUID = 1L;
	
	private static final int CODE = 1;

	public TooManyContainerIdsReason(String message) {
		super(CODE, message);
	}
	
}

