package com.github.skjolber.packing.validator.load;

import java.util.List;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;

/**
 * A {@link LoadValidator} that delegates sequentially to a fixed list of validators.
 *
 * <p>Evaluation continues until all validators have been checked, even if an earlier
 * one already failed, so that all violations across all constraints are collected in
 * a single pass.
 *
 * <p>An empty delegate list is allowed; in that case {@link #isValid} always returns
 * {@code true}.
 *
 * @see DefaultLoadValidatorBuilder
 */
public class CompositeLoadValidator implements LoadValidator {

	private final List<LoadValidator> validators;

	/**
	 * Creates a composite validator wrapping the given list of delegates.
	 *
	 * @param validators the load validators to evaluate in order; must not be {@code null}
	 */
	public CompositeLoadValidator(List<LoadValidator> validators) {
		if(validators == null) {
			throw new IllegalArgumentException("validators must not be null");
		}
		this.validators = validators;
	}

	/**
	 * Returns the list of delegate validators.
	 *
	 * @return the delegate validators
	 */
	public List<LoadValidator> getValidators() {
		return validators;
	}

	/**
	 * Evaluates all delegate validators, collecting reasons from each one.
	 *
	 * <p>All validators are always invoked regardless of prior failures, so that
	 * the {@code reasons} list receives the complete set of violations.
	 *
	 * @param list the placements to validate
	 * @param reasons mutable list to receive violation reasons
	 * @return {@code true} if all delegates returned {@code true}; {@code false} if any failed
	 */
	@Override
	public boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons) {
		boolean valid = true;

		for(LoadValidator validator : validators) {
			if(!validator.isValid(list, reasons)) {
				valid = false;
			}
		}

		return valid;
	}
}
