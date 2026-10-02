package com.github.skjolber.packing.validator.reasons;

public class ContainerCountTooHighReason extends AbstractValidatorResultReason {

	private static final long serialVersionUID = 1L;
	
	private static final int CODE = 6;
	
	public ContainerCountTooHighReason(String message) {
		super(CODE, message);
	}
	
}
