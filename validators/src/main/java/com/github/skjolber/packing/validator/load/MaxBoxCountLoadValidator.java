package com.github.skjolber.packing.validator.load;

import java.util.List;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadBoxCountReason;

/**
 * Validates that the number of box levels stacked on top of each placement does not exceed
 * the limit set by {@link BoxStackValue#getMaxLoadBoxCount()}.
 *
 * <p>Only placements for which {@link BoxStackValue#isMaxLoadBoxCount()} returns {@code true}
 * are checked. The depth is measured as the longest chain of supportees above the placement,
 * consistent with the depth semantics used by
 * {@link Placement#isWithinMaxLoadBoxCount(int)}.
 *
 * @see BoxStackValue#getMaxLoadBoxCount()
 * @see Placement#getSupportees()
 */
public class MaxBoxCountLoadValidator implements LoadValidator {

	/**
	 * {@inheritDoc}
	 *
	 * <p>Iterates all placements and adds an {@link ExcessiveLoadBoxCountReason} for each one
	 * whose supportee stack depth exceeds the permitted maximum.
	 *
	 * @return {@code true} if no box-count constraints are violated; {@code false} otherwise
	 */
	@Override
	public boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons) {
		boolean valid = true;

		for(Placement placement : list) {
			BoxStackValue stackValue = placement.getStackValue();

			if(!stackValue.isMaxLoadBoxCount()) {
				continue;
			}

			int depth = supporteeDepth(placement);
			int maxLoadBoxCount = stackValue.getMaxLoadBoxCount();

			if(depth > maxLoadBoxCount) {
				reasons.add(new ExcessiveLoadBoxCountReason(placement, depth, maxLoadBoxCount));
				valid = false;
			}
		}

		return valid;
	}

	/**
	 * Returns the maximum depth of the supportee subtree rooted at {@code placement},
	 * i.e. the longest chain of boxes directly or indirectly resting on top of it.
	 * A placement with no supportees has depth 0.
	 *
	 * @param placement the placement whose supportee depth to measure
	 * @return depth of the supportee subtree (0 if no boxes are on top)
	 */
	private int supporteeDepth(Placement placement) {
		List<PlacementLoad> supportees = placement.getSupportees();
		if(supportees.isEmpty()) {
			return 0;
		}

		int max = 0;
		for(PlacementLoad load : supportees) {
			int depth = 1 + supporteeDepth(load.getPlacement());
			if(depth > max) {
				max = depth;
			}
		}
		return max;
	}
}
