package com.github.skjolber.packing.validator;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.reasons.BlockedExtractionReason;

/**
 * Validates the extraction order of the boxes of a container (see
 * {@link com.github.skjolber.packing.api.BoxItem#withExtractionOrder(int)}): a box can be extracted before the boxes
 * with a later extraction order without moving them, as none of them rests on it, or is in its path to the container's
 * opening (see {@link ContainerAccess}).
 */
public class ExtractionOrderValidator {

	/**
	 * @param container the container
	 * @param reasons reasons are added here
	 * @return true if valid
	 */
	public boolean validate(Container container, List<ValidatorResultReason> reasons) {
		boolean valid = true;
		ContainerAccess access = container.getAccess();
		List<Placement> placements = container.getStack().getPlacements();
		for (int i = 0; i < placements.size(); i++) {
			Placement a = placements.get(i);
			int order = getExtractionOrder(a);
			for (int j = 0; j < placements.size(); j++) {
				Placement b = placements.get(j);
				// a is extracted before b: b must be insertable before a
				if(order < getExtractionOrder(b) && a.mustPrecede(b, access)) {
					reasons.add(new BlockedExtractionReason(a, b));
					valid = false;
				}
			}
		}
		return valid;
	}

	private static int getExtractionOrder(Placement placement) {
		BoxItem boxItem = placement.getBoxItem();
		return boxItem != null ? boxItem.getExtractionOrder() : 0;
	}
}
