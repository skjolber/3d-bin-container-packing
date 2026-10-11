package com.github.skjolber.packing.api.validator;

import java.util.List;

/**
 * 
 * Validator result: whether the packager result is valid, why not, and which of the placement checks were performed.
 * <p>
 * A valid result is only as good as the checks which were performed. The {@linkplain #isLoadValidated() load} and
 * {@linkplain #isStabilityValidated() stability} flags tell which placement checks the validation covered, so that a caller can tell a
 * result which was checked for the load constraints of its boxes from one which was not.
 * 
 */

public class ValidatorResult {

	protected final long duration;
	protected final boolean valid;
	protected final boolean timeout;
	protected final List<ValidatorResultReason> reasons;
	protected final boolean loadValidated;
	protected final boolean stabilityValidated;
	protected final boolean loadConstrained;

	/**
	 * Constructor for a result which does not report which placement checks were performed (for example of a validator which has none):
	 * {@linkplain #isLoadValidated()}, {@linkplain #isStabilityValidated()} and {@linkplain #hasLoadConstraints()} are all false.
	 * 
	 * @param duration duration in milliseconds
	 * @param valid whether the packager result is valid
	 * @param timeout whether the validation was interrupted
	 * @param reasons why the packager result is not valid
	 */

	public ValidatorResult(long duration, boolean valid, boolean timeout, List<ValidatorResultReason> reasons) {
		this(duration, valid, timeout, reasons, false, false, false);
	}

	/**
	 * @param duration duration in milliseconds
	 * @param valid whether the packager result is valid
	 * @param timeout whether the validation was interrupted
	 * @param reasons why the packager result is not valid
	 * @param loadValidated see {@linkplain #isLoadValidated()}
	 * @param stabilityValidated see {@linkplain #isStabilityValidated()}
	 * @param loadConstrained see {@linkplain #hasLoadConstraints()}
	 */

	public ValidatorResult(long duration, boolean valid, boolean timeout, List<ValidatorResultReason> reasons, boolean loadValidated, boolean stabilityValidated, boolean loadConstrained) {
		this.duration = duration;
		this.valid = valid;
		this.timeout = timeout;
		this.reasons = reasons;
		this.loadValidated = loadValidated;
		this.stabilityValidated = stabilityValidated;
		this.loadConstrained = loadConstrained;
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

	/**
	 * Whether the load constraints of the boxes (maximum load weight, pressure and box count, and identical boxes only) were validated.
	 * <p>
	 * True if load validation was performed on <b>every</b> container of the packager result, which includes the case where no box declares
	 * a load constraint: there was nothing to check, so the validation passed vacuously. That case is told apart by
	 * {@linkplain #hasLoadConstraints()}: <code>isLoadValidated() &amp;&amp; !hasLoadConstraints()</code>.
	 * <p>
	 * False if load validation was skipped (opted out of, or a placement validator which does not validate loads), or the validation did not
	 * get as far as every container: it was interrupted ({@linkplain #isTimeout()}), or an earlier check rejected the result (validation stops
	 * at the first rejection). The flag is therefore only a statement about the checks which were made for a valid result.
	 * 
	 * @return true if load validation was performed on every container
	 */

	public boolean isLoadValidated() {
		return loadValidated;
	}

	/**
	 * Whether a stability rule (for example full support, or the centre of gravity) was validated: true if a stability validator was
	 * configured, and it was applied to <b>every</b> container of the packager result. Stability is a policy, not a fact of the input, so
	 * unlike the load constraints it is not validated unless asked for.
	 * <p>
	 * False if no stability validator was configured, or the validation did not get as far as every container (see {@linkplain #isLoadValidated()}).
	 * 
	 * @return true if stability validation was performed on every container
	 */

	public boolean isStabilityValidated() {
		return stabilityValidated;
	}

	/**
	 * Whether load validation had something to check: at least one box placed in a container which was load validated declares a load
	 * constraint (see {@linkplain #isLoadValidated()}). If load validation was performed but this is false, it passed vacuously.
	 * <p>
	 * Always false if load validation was not performed, as the boxes are not inspected then.
	 * 
	 * @return true if at least one validated box declares a load constraint
	 */

	public boolean hasLoadConstraints() {
		return loadConstrained;
	}
}
