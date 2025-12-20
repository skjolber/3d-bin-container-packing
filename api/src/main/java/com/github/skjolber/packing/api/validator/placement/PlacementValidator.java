package com.github.skjolber.packing.api.validator.placement;

import java.util.List;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;

public interface PlacementValidator {

	boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons);
	
}
