package com.github.skjolber.packing.api.validator.placement.load;

import java.util.List;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;

public interface LoadValidator {

	boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons);
	
}
