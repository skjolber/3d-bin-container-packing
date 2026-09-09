package com.github.skjolber.packing.validator;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;
import com.github.skjolber.packing.api.validator.placement.PlacementValidator;
import com.github.skjolber.packing.api.validator.placement.StabilityValidator;

public class DefaultPlacementValidator implements PlacementValidator {

	private Container container;
	private LoadValidator loadValidator;
	private StabilityValidator stabilityValidator;
	
	public DefaultPlacementValidator(Container container, LoadValidator loadValidator, StabilityValidator stabilityValidator) {
		super();
		this.container = container;
		this.loadValidator = loadValidator;
		this.stabilityValidator = stabilityValidator;
	}
	
	public Container getContainer() {
		return container;
	}
	
	public LoadValidator getLoadValidator() {
		return loadValidator;
	}
	
	public StabilityValidator getStabilityValidator() {
		return stabilityValidator;
	}

	@Override
	public boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons) {
		
		// detect
		if(loadValidator != null) {
			if(!loadValidator.isValid(list, reasons)) {
				return false;
			}
		}

		if(stabilityValidator != null) {
			if(!stabilityValidator.isValid(list, reasons)) {
				return false;
			}
		}

		return true;
	}

}
