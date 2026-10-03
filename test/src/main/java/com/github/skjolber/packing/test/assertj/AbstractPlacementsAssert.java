package com.github.skjolber.packing.test.assertj;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;
import com.github.skjolber.packing.api.validator.placement.PlacementValidator;
import com.github.skjolber.packing.api.validator.placement.StabilityValidator;

/**
 * Assertions on a list of placements, typically the placements of a stack, against load,
 * stability and placement validators.
 */
public abstract class AbstractPlacementsAssert<SELF extends AbstractPlacementsAssert<SELF, ACTUAL>, ACTUAL extends List<Placement>>
		extends AbstractObjectAssert<SELF, ACTUAL> {

	private interface Validation {
		boolean isValid(List<Placement> placements, List<ValidatorResultReason> reasons);
	}

	protected AbstractPlacementsAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF isAcceptedBy(LoadValidator validator) {
		return isAccepted(validator::isValid, validator);
	}

	public SELF isAcceptedBy(StabilityValidator validator) {
		return isAccepted(validator::isValid, validator);
	}

	public SELF isAcceptedBy(PlacementValidator validator) {
		return isAccepted(validator::isValid, validator);
	}

	/**
	 * @param reasons expected reason types, in order; empty for any non-empty list of reasons
	 */
	@SafeVarargs
	public final SELF isRejectedBy(LoadValidator validator, Class<? extends ValidatorResultReason>... reasons) {
		return isRejected(validator::isValid, validator, reasons);
	}

	/**
	 * @param reasons expected reason types, in order; empty for any non-empty list of reasons
	 */
	@SafeVarargs
	public final SELF isRejectedBy(StabilityValidator validator, Class<? extends ValidatorResultReason>... reasons) {
		return isRejected(validator::isValid, validator, reasons);
	}

	/**
	 * @param reasons expected reason types, in order; empty for any non-empty list of reasons
	 */
	@SafeVarargs
	public final SELF isRejectedBy(PlacementValidator validator, Class<? extends ValidatorResultReason>... reasons) {
		return isRejected(validator::isValid, validator, reasons);
	}

	private SELF isAccepted(Validation validation, Object validator) {
		isNotNull();
		List<ValidatorResultReason> reasons = new ArrayList<>();
		boolean valid = validation.isValid(actual, reasons);
		if(!valid || !reasons.isEmpty()) {
			failWithMessage("Expected placements to be accepted by %s, got valid=%s with reasons %s", validator.getClass().getSimpleName(), valid, reasons);
		}
		return myself;
	}

	private SELF isRejected(Validation validation, Object validator, Class<? extends ValidatorResultReason>[] expected) {
		isNotNull();
		List<ValidatorResultReason> reasons = new ArrayList<>();
		if(validation.isValid(actual, reasons)) {
			failWithMessage("Expected placements to be rejected by %s", validator.getClass().getSimpleName());
		}
		if(reasons.isEmpty()) {
			failWithMessage("Expected rejection reasons from %s", validator.getClass().getSimpleName());
		}
		if(expected.length > 0) {
			if(reasons.size() != expected.length) {
				failWithMessage("Expected %s reason(s), got %s", expected.length, reasons);
			}
			for(int i = 0; i < expected.length; i++) {
				if(!expected[i].isInstance(reasons.get(i))) {
					failWithMessage("Expected reason %s to be %s, got %s", i, expected[i].getSimpleName(), reasons.get(i));
				}
			}
		}
		return myself;
	}
}
