package com.github.skjolber.packing.validator;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.reasons.ContainerPriorityReason;

/**
 * Validates the container priorities of the boxes (see
 * {@link com.github.skjolber.packing.api.BoxItem#withContainerPriority(int)}): a box must not be in a later container
 * than a box with a higher priority (a lower value).
 */
public class ContainerPriorityValidator {

	/**
	 * @param containers the containers of a result, in order
	 * @param reasons reasons are added here
	 * @return true if valid
	 */
	public boolean validate(List<Container> containers, List<ValidatorResultReason> reasons) {
		boolean valid = true;
		// the box with the highest priority value so far, and its container
		Placement highest = null;
		int highestContainer = -1;
		for (int i = 0; i < containers.size(); i++) {
			List<Placement> placements = containers.get(i).getStack().getPlacements();
			if(highest != null) {
				for (Placement placement : placements) {
					if(getContainerPriority(placement) < getContainerPriority(highest)) {
						reasons.add(new ContainerPriorityReason(placement, i, highest, highestContainer));
						valid = false;
					}
				}
			}
			for (Placement placement : placements) {
				if(highest == null || getContainerPriority(placement) > getContainerPriority(highest)) {
					highest = placement;
					highestContainer = i;
				}
			}
		}
		return valid;
	}

	private static int getContainerPriority(Placement placement) {
		BoxItem boxItem = placement.getBoxItem();
		return boxItem != null ? boxItem.getContainerPriority() : 0;
	}
}
