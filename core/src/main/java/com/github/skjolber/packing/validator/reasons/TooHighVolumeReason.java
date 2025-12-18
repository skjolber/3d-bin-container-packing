package com.github.skjolber.packing.validator.reasons;

/**
 * 
 * There is more box item types than expected.
 * 
 */


public class TooHighVolumeReason extends AbstractValidatorResultReason {

	private static final long serialVersionUID = 1L;
	
	private static final int CODE = 4;

	public TooHighVolumeReason(String message) {
		super(CODE, message);
	}
	
}

