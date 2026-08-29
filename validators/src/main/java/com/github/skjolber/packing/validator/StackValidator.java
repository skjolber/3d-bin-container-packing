package com.github.skjolber.packing.validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.reasons.BoxOutsideContainerReason;
import com.github.skjolber.packing.validator.reasons.BoxesIntersectReason;
import com.github.skjolber.packing.validator.reasons.TooHighVolumeReason;
import com.github.skjolber.packing.validator.reasons.TooHighWeightReason;
import com.github.skjolber.packing.validator.reasons.UnknownContainerIdReason;

public class StackValidator {

	public boolean validate(List<ValidatorContainerItem> containers, PackagerResult result, List<ValidatorResultReason> reasons) {
		
		Map<String, ValidatorContainerItem> referenceContainersById = new HashMap<>();
		for (ValidatorContainerItem validatorContainerItem : containers) {
			referenceContainersById.put(validatorContainerItem.getContainer().getId(), validatorContainerItem);
		}
		
		for (Container container : result.getContainers()) {
			ValidatorContainerItem referenceContainerItem = referenceContainersById.get(container.getId());
			if(referenceContainerItem == null) {
				reasons.add(new UnknownContainerIdReason("Unknown container " + container.getId()));
				return false;
			}
			
			Container referenceContainer = referenceContainerItem.getContainer();
			
			Stack stack = container.getStack();
			if(stack.getVolume() > referenceContainer.getMaxLoadVolume()) {
				reasons.add(new TooHighVolumeReason("Expected maximum " + referenceContainer.getMaxLoadVolume() + ", found " + stack.getVolume()));
				return false;
			}
			
			if(stack.getWeight() > referenceContainer.getMaxLoadWeight()) {
				reasons.add(new TooHighWeightReason("Expected maximum " + referenceContainer.getMaxLoadWeight() + ", found " + stack.getWeight()));
				return false;
			}
			
			List<Placement> placements = stack.getPlacements();
			for (Placement placement : placements) {
				if(!isInside(referenceContainer, placement)) {
					reasons.add(new BoxOutsideContainerReason("Box " + placement.getBox().getId() + " not placed within load limits"));
					return false;
				}
			}
			
			// check if boxes intersect
			for(int i = 0; i < placements.size(); i++) {
				Placement placement1 = placements.get(i);
				for(int k = 0; k < placements.size(); k++) {
					if(i == k) {
						continue;
					}
					Placement placement2 = placements.get(k);
					
					if(placement1.intersects(placement2)) {
						reasons.add(new BoxesIntersectReason(placement1.getBox().getId() + " intersects " + placement2.getBox().getId()));
						return false;
					}
				}
			}
		}
		
		return true;
	}

	private boolean isInside(Container container, Placement placement) {
		if(placement.getAbsoluteEndX() >= container.getLoadDx()) {
			return false;
		}
		if(placement.getAbsoluteEndY() >= container.getLoadDy()) {
			return false;
		}
		if(placement.getAbsoluteEndZ() >= container.getLoadDz()) {
			return false;
		}

		if(placement.getAbsoluteX() < 0) {
			return false;
		}
		if(placement.getAbsoluteY() < 0) {
			return false;
		}
		if(placement.getAbsoluteZ() < 0) {
			return false;
		}

		return true;
	}
	
}
