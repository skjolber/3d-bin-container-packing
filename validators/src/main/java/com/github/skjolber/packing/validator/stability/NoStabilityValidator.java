package com.github.skjolber.packing.validator.stability;

import java.util.List;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.StabilityValidator;

public class NoStabilityValidator implements StabilityValidator {

	@Override
	public boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons) {
		return true;
	}

}
