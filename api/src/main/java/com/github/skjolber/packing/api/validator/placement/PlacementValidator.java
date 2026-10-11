package com.github.skjolber.packing.api.validator.placement;

import java.util.List;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;

public interface PlacementValidator {

	boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons);

	/**
	 * Whether this validator validates the load constraints of the boxes. This is reported by the validator result
	 * (see {@linkplain com.github.skjolber.packing.api.validator.ValidatorResult#isLoadValidated()}).
	 * <p>
	 * Defaults to false: an implementation which validates loads should say so. True also means that nothing was to be checked if no box
	 * declares a load constraint.
	 * 
	 * @return true if the load constraints of the boxes are validated
	 */

	default boolean isLoadValidated() {
		return false;
	}

	/**
	 * Whether this validator validates a stability rule. This is reported by the validator result
	 * (see {@linkplain com.github.skjolber.packing.api.validator.ValidatorResult#isStabilityValidated()}).
	 * <p>
	 * Defaults to false: an implementation which validates stability should say so.
	 * 
	 * @return true if a stability rule is validated
	 */

	default boolean isStabilityValidated() {
		return false;
	}

}
