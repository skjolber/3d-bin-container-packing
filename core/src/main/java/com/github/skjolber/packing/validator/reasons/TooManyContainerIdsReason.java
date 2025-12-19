package com.github.skjolber.packing.validator.reasons;

/**
 * Validation reason indicating that there are more container IDs than expected
 * or that some container IDs are unknown or otherwise invalid.
 */
public class TooManyContainerIdsReason extends AbstractValidatorResultReason {

	private static final long serialVersionUID = 1L;
	
	private static final int CODE = 1;

	public TooManyContainerIdsReason(String message) {
		super(CODE, message);
	}
	
}

