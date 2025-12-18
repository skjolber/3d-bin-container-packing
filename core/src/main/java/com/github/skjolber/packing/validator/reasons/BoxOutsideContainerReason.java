package com.github.skjolber.packing.validator.reasons;

public class BoxOutsideContainerReason extends AbstractValidatorResultReason {

	private static final long serialVersionUID = 1L;
	
	private static final int CODE = 9;
	
	public BoxOutsideContainerReason(String message) {
		super(CODE, message);
	}
	
}
