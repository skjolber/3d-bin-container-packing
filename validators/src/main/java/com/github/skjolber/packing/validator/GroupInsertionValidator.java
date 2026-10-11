package com.github.skjolber.packing.validator;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.reasons.InterleavedGroupReason;

/**
 * Validates that the boxes of each box item group of a container are inserted together: in the order of the
 * placements, no box of another group is between them.
 */
public class GroupInsertionValidator {

	/**
	 * @param container the container
	 * @param reasons reasons are added here
	 * @return true if valid
	 */
	public boolean validate(Container container, List<ValidatorResultReason> reasons) {
		boolean valid = true;
		List<Placement> placements = container.getStack().getPlacements();
		// the groups whose boxes were followed by a box of another group
		Set<Object> closed = new HashSet<>();
		for (int i = 1; i < placements.size(); i++) {
			Object previous = getGroupKey(placements.get(i - 1));
			Object group = getGroupKey(placements.get(i));
			if(Objects.equals(previous, group)) {
				continue;
			}
			if(previous != null) {
				closed.add(previous);
			}
			if(group != null && closed.contains(group)) {
				reasons.add(new InterleavedGroupReason(placements.get(i), placements.get(i - 1)));
				valid = false;
			}
		}
		return valid;
	}

	private static Object getGroupKey(Placement placement) {
		BoxItem boxItem = placement.getBoxItem();
		return boxItem != null ? boxItem.getGroupKey() : null;
	}
}
