package com.github.skjolber.packing.validator;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.PlacementValidator;

public class DefaultPlacementValidator implements PlacementValidator {

	private Container container;

	public DefaultPlacementValidator(Container container) {
		super();
		this.container = container;
	}
	
	public Container getContainer() {
		return container;
	}

	@Override
	public boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons) {
		return true;
	}

}
