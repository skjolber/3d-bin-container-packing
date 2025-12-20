package com.github.skjolber.packing.api.validator;

import java.util.List;

/**
 * 
 * Packager result. 
 * 
 */

public class ValidatorResult {

	protected final long duration;
	protected final boolean valid;
	protected final boolean timeout;
	protected final List<ValidatorResultReason> reasons;

	public ValidatorResult(long duration, boolean valid, boolean timeout, List<ValidatorResultReason> reasons) {
		this.duration = duration;
		this.valid = valid;
		this.timeout = timeout;
		this.reasons = reasons;
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

	public List<ValidatorResultReason> getReasons() {
		return reasons;
	}
}
