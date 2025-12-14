package com.github.skjolber.packing.api;

/**
 * 
 * Packager result. 
 * 
 */

public class ValidatorResult {

	protected final long duration;
	protected final boolean valid;
	protected final boolean timeout;

	public ValidatorResult(long duration, boolean valid, boolean timeout) {
		this.duration = duration;
		this.valid = valid;
		this.timeout = timeout;
	}

	public long getDuration() {
		return duration;
	}

	public boolean isValid() {
		return valid;
	}
	
	public boolean isTimeout() {
		return timeout;
	}

}
