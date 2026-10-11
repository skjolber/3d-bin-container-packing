package com.github.skjolber.packing.validator;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.reasons.BoxIntersectsObstacleReason;

/**
 * Validates that no box takes up space which belongs to an obstacle of its container, that is, that the obstacles are left empty.
 * The obstacles are those the container carries ({@link Container#getObstacles()}, which the packagers keep in their result
 * containers), as in {@link InsertionOrderValidator}. Box against box intersection is validated by {@link StackValidator}.
 */
public class ObstacleIntersectionValidator {

	/**
	 * @param container the container, with its placements and obstacles
	 * @param reasons reasons are added here, one for each box which intersects an obstacle
	 * @return true if valid
	 */
	public boolean validate(Container container, List<ValidatorResultReason> reasons) {
		List<Placement> obstacles = container.getObstacles();
		if(obstacles.isEmpty()) {
			return true;
		}
		boolean valid = true;
		for (Placement placement : container.getStack().getPlacements()) {
			for (Placement obstacle : obstacles) {
				if(placement.intersects(obstacle)) {
					reasons.add(new BoxIntersectsObstacleReason(placement, obstacle));
					valid = false;
				}
			}
		}
		return valid;
	}
}
