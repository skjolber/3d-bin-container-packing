package com.github.skjolber.packing.validator.reasons;

public class BoxesOutsideContainerReason extends AbstractValidatorResultReason {

	private static final long serialVersionUID = 1L;
	
	private static final int CODE = 9;
	
	public BoxesOutsideContainerReason(String message) {
		super(CODE, message);
	}
	
}
