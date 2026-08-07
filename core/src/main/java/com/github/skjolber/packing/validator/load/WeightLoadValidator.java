package com.github.skjolber.packing.validator.load;

import java.util.List;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.load.LoadValidator;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadWeightReason;

/**
 * Validates that the accumulated load weight on top of each placement does not exceed
 * the limit set by {@link BoxStackValue#getMaxLoadWeight()}.
 *
 * <p>Only placements for which {@link BoxStackValue#isMaxLoadWeight()} returns {@code true}
 * are checked. The load weight is computed independently by traversing the supportee graph
 * from each constrained placement — it does not rely on the cached {@code loadWeight} field
 * maintained by the placement graph.
 *
 * <p>The computation mirrors the proportional weight distribution used during load
 * propagation: when a box above is shared between multiple supporters, its weight
 * contribution to this placement is scaled by {@code overlapArea / supportee.supportedArea}.
 *
 * <p>If pressure is also configured on a stack value, prefer {@link MaxPressureLoadValidator}
 * as pressure takes precedence per the {@link BoxStackValue} contract.
 *
 * @see BoxStackValue#getMaxLoadWeight()
 * @see Placement#getSupportees()
 */
public class WeightLoadValidator implements LoadValidator {

	/**
	 * {@inheritDoc}
	 *
	 * <p>Iterates all placements that declare a max load weight and adds an
	 * {@link ExcessiveLoadWeightReason} for each one whose computed load weight
	 * exceeds the permitted maximum.
	 *
	 * @return {@code true} if no weight constraints are violated; {@code false} otherwise
	 */
	@Override
	public boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons) {
		boolean valid = true;

		for(Placement placement : list) {
			BoxStackValue stackValue = placement.getStackValue();

			if(!stackValue.isMaxLoadWeight()) {
				continue;
			}

			long loadWeight = accumulateWeight(placement, 1000L) / 1000L;
			long maxLoadWeight = stackValue.getMaxLoadWeight();

			if(loadWeight > maxLoadWeight) {
				reasons.add(new ExcessiveLoadWeightReason(placement, loadWeight, maxLoadWeight));
				valid = false;
			}
		}

		return valid;
	}

	/**
	 * Recursively accumulates the total weight resting on top of {@code placement},
	 * proportionally attributing the weight of shared supportees.
	 *
	 * <p>The {@code share} parameter is a fixed-point multiplier (1000 at the root)
	 * used to avoid integer truncation when distributing weight across multiple supporters.
	 * When a supportee is shared, its weight contribution to this placement is scaled by
	 * {@code overlapArea / supportee.supportedArea}.
	 *
	 * @param placement the placement whose supportee weight to accumulate
	 * @param share fixed-point multiplier for this subtree (1000 at root)
	 * @return total accumulated weight above {@code placement}, scaled by the initial {@code share}
	 */
	static long accumulateWeight(Placement placement, long share) {
		long total = 0;

		for(PlacementLoad supporteeLink : placement.getSupportees()) {
			Placement supportee = supporteeLink.getPlacement();

			// Weight of this supportee box, scaled by our share of its total supported area
			long supporteeArea = supportee.getSupportedArea();
			long supporteeShare = (supporteeArea > 0)
					? (share * supporteeLink.getArea()) / supporteeArea
					: share;

			total += (long) supportee.getWeight() * supporteeShare;

			// Recurse: add the weight of everything above the supportee, at the same proportion
			total += accumulateWeight(supportee, supporteeShare);
		}

		return total;
	}
}
