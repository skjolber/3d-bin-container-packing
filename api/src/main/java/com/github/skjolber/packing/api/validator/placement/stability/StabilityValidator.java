package com.github.skjolber.packing.api.validator.placement.stability;

import java.util.List;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;

public interface StabilityValidator {

	boolean isStable(List<Placement> list, List<ValidatorResultReason> reasons);
	
}
