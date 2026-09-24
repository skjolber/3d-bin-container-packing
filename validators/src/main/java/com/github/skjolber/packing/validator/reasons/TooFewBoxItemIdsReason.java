package com.github.skjolber.packing.validator.reasons;

/**
 * 
 * Some box item types are missing.
 * 
 */

public class TooFewBoxItemIdsReason extends AbstractValidatorResultReason {

	private static final long serialVersionUID = 1L;
	
	private static final int CODE = 5;

	public TooFewBoxItemIdsReason(String message) {
		super(CODE, message);
	}
	
}
