package com.github.skjolber.packing.validator.reasons;

/**
 * 
 * Represents a validation failure where the total volume is higher than allowed.
 * 
 */


public class TooHighVolumeReason extends AbstractValidatorResultReason {

	private static final long serialVersionUID = 1L;
	
	private static final int CODE = 4;

	public TooHighVolumeReason(String message) {
		super(CODE, message);
	}
	
}

