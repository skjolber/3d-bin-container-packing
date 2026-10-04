package com.github.skjolber.packing.validator.load;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.validator.SupportGraph;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;
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
 * @see SupportGraph
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
		SupportGraph graph = new SupportGraph(list);
		Map<Placement, Double> weightAbove = new IdentityHashMap<>();

		for(Placement placement : list) {
			BoxStackValue stackValue = placement.getStackValue();

			if(!stackValue.isMaxLoadWeight()) {
				continue;
			}

			double loadWeight = accumulateWeight(graph, placement, 1.0, weightAbove);
			long maxLoadWeight = stackValue.getMaxLoadWeight();

			if(loadWeight > maxLoadWeight) {
				reasons.add(new ExcessiveLoadWeightReason(placement, loadWeight, maxLoadWeight));
				valid = false;
			}
		}

		return valid;
	}

	/**
	 * Accumulates the total weight resting on top of {@code placement}, proportionally attributing
	 * the weight of shared supportees.
	 * <p>
	 * When a supportee is shared, its weight contribution to this placement is scaled by
	 * {@code overlapArea / supportee.supportedArea}.
	 *
	 * @param placement the placement whose supportee weight to accumulate
	 * @param share fractional multiplier (1.0 for the placement itself)
	 * @return total accumulated weight above {@code placement}, times {@code share}
	 */
	static double accumulateWeight(SupportGraph graph, Placement placement, double share) {
		return accumulateWeight(graph, placement, share, new IdentityHashMap<>());
	}

	/**
	 * @param weightAbove the total weight above each placement visited so far; each placement is
	 *        calculated once, as one can be reached through several paths (and walking every path
	 *        grows exponentially with the stack height)
	 */
	static double accumulateWeight(SupportGraph graph, Placement placement, double share, Map<Placement, Double> weightAbove) {
		return share * weightAbove(graph, placement, weightAbove);
	}

	private static double weightAbove(SupportGraph graph, Placement placement, Map<Placement, Double> weightAbove) {
		Double known = weightAbove.get(placement);
		if(known != null) {
			return known;
		}
		double total = 0.0;
		for(PlacementLoad supporteeLink : graph.getSupportees(placement)) {
			Placement supportee = supporteeLink.getPlacement();
			// weight of this supportee box and everything above it, scaled by our share of its total supported area
			long supporteeArea = graph.getSupportedArea(supportee);
			double supporteeShare = (supporteeArea > 0) ? (double) supporteeLink.getArea() / supporteeArea : 1.0;
			total += (supportee.getWeight() + weightAbove(graph, supportee, weightAbove)) * supporteeShare;
		}
		weightAbove.put(placement, total);
		return total;
	}
}
