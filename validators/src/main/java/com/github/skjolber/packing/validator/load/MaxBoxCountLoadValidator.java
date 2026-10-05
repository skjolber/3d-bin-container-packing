package com.github.skjolber.packing.validator.load;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Unloading;
import com.github.skjolber.packing.validator.SupportGraph;
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
 * @see SupportGraph
 */
public class MaxBoxCountLoadValidator implements LoadValidator {

	/** How the boxes are unloaded, see {@link SupportGraph} */
	protected final Unloading unloading;

	/**
	 * Validator for boxes unloaded in any order, see {@link Unloading#ANY_ORDER}.
	 */
	public MaxBoxCountLoadValidator() {
		this(Unloading.ANY_ORDER);
	}

	/**
	 * @param unloading how the boxes are unloaded
	 */
	public MaxBoxCountLoadValidator(Unloading unloading) {
		this.unloading = unloading;
	}


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
		Map<Placement, Integer> depths = new IdentityHashMap<>();

		SupportGraph graph = new SupportGraph(list, unloading);
		for(Placement placement : list) {
			BoxStackValue stackValue = placement.getStackValue();

			if(!stackValue.isMaxLoadBoxCount()) {
				continue;
			}

			int depth = supporteeDepth(graph, placement, depths);
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
	 * @param depths the depth of each placement measured so far; each placement is measured once, as
	 *        one can be reached through several paths (and walking every path grows exponentially)
	 * @return depth of the supportee subtree (0 if no boxes are on top)
	 */
	private int supporteeDepth(SupportGraph graph, Placement placement, Map<Placement, Integer> depths) {
		List<PlacementLoad> supportees = graph.getSupportees(placement);
		if(supportees.isEmpty()) {
			return 0;
		}
		Integer known = depths.get(placement);
		if(known != null) {
			return known;
		}

		int max = 0;
		for(PlacementLoad load : supportees) {
			int depth = 1 + supporteeDepth(graph, load.getPlacement(), depths);
			if(depth > max) {
				max = depth;
			}
		}
		depths.put(placement, max);
		return max;
	}
}
