package com.github.skjolber.packing.validator;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.reasons.BlockedInsertionReason;
import com.github.skjolber.packing.validator.reasons.InsertedBeforeSupporterReason;

/**
 * Validates that the order of the placements of a stack is a possible insertion order, see {@link ContainerAccess}:
 * each box is inserted after the boxes it rests on, and its path from the container's opening is free. The boxes already
 * in a container (obstacles) are inserted first.
 */
public class InsertionOrderValidator {

	/**
	 * Validate the placements of a container, which are inserted after the boxes already in it (its obstacles).
	 *
	 * @param container the container
	 * @param reasons reasons are added here
	 * @return true if valid
	 */
	public boolean validate(Container container, List<ValidatorResultReason> reasons) {
		boolean valid = validate(container.getStack().getPlacements(), container.getAccess(), reasons);
		List<Placement> obstacles = container.getObstacles();
		if(!obstacles.isEmpty()) {
			ContainerAccess access = container.getAccess();
			for (Placement placement : container.getStack().getPlacements()) {
				for (Placement obstacle : obstacles) {
					if(obstacle.restsOn(placement)) {
						reasons.add(new InsertedBeforeSupporterReason(obstacle, placement));
						valid = false;
					} else if(placement.isBlockedBy(obstacle, access)) {
						reasons.add(new BlockedInsertionReason(placement, obstacle));
						valid = false;
					}
				}
			}
		}
		return valid;
	}

	/**
	 * @param placements placements in insertion order
	 * @param access how boxes get into the container
	 * @param reasons reasons are added here
	 * @return true if valid
	 */
	public boolean validate(List<Placement> placements, ContainerAccess access, List<ValidatorResultReason> reasons) {
		boolean valid = true;
		for (int i = 0; i < placements.size(); i++) {
			Placement placement = placements.get(i);
			for (int j = i + 1; j < placements.size(); j++) {
				Placement later = placements.get(j);
				if(placement.restsOn(later)) {
					reasons.add(new InsertedBeforeSupporterReason(placement, later));
					valid = false;
				} else if(later.isBlockedBy(placement, access)) {
					// the later box would have to pass this one
					reasons.add(new BlockedInsertionReason(later, placement));
					valid = false;
				}
			}
		}
		return valid;
	}
}
