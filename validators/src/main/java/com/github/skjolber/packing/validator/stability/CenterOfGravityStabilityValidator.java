package com.github.skjolber.packing.validator.stability;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.validator.SupportGraph;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.StabilityValidator;
import com.github.skjolber.packing.validator.stability.reasons.UnstableStackCenterOfGravityReason;

/**
 * Validates that the effective centre of gravity of each placement's entire vertical
 * stack — this box plus all boxes above it, weighted proportionally by their shared
 * support area — lies within the bounding box of the union of its support contact patches.
 *
 * <p>This is stricter than {@link CenterOfGravitySupportStabilityValidator} because it
 * accounts for the off-centre weight contribution of every box stacked above. A heavy
 * overhanging box on top can shift the effective centre of gravity outside the support
 * region even when the bottom box's own centre of gravity is within bounds.
 *
 * <p>A placement with no supporters is considered stable only when it rests on the
 * container floor ({@code z == 0}).
 *
 * @see SupportGraph
 */
public class CenterOfGravityStabilityValidator implements StabilityValidator {

	/**
	 * {@inheritDoc}
	 *
	 * <p>Adds an {@link UnstableStackCenterOfGravityReason} for every placement whose
	 * stack centre of gravity falls outside the support region of its direct supporters.
	 *
	 * @return {@code true} if every placement's stack is stable; {@code false} otherwise
	 */
	@Override
	public boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons) {
		boolean valid = true;

		SupportGraph graph = new SupportGraph(list);
		for(Placement placement : list) {
			if(!isPlacementStable(graph, placement)) {
				reasons.add(new UnstableStackCenterOfGravityReason(placement));
				valid = false;
			}
		}

		return valid;
	}

	/**
	 * Determines whether {@code placement} is stable considering both its supporters
	 * and the weight of every box stacked above it (its supportees, recursively).
	 *
	 * <p>The effective centre of mass (CoM) is computed as the weighted average of
	 * the configured centres of gravity of this box and all boxes in its supportee sub-tree.
	 * When a box above is shared between multiple supporters (split load), its
	 * contribution to this sub-tree is scaled by
	 * {@code overlapArea / supportee.supportedArea}, matching the same proportion
	 * used during load propagation.
	 *
	 * <p>The effective CoM is tested against the axis-aligned bounding box of the
	 * union of all support contact patches.
	 *
	 * @param graph the support graph of the placements
	 * @param placement the placement to check
	 * @return {@code true} if the effective centre of mass of the stack lies within
	 *         the support region
	 */
	public static boolean isPlacementStable(SupportGraph graph, Placement placement) {
		List<PlacementLoad> supporters = graph.getSupporters(placement);

		if(supporters.isEmpty()) {
			return placement.getAbsoluteZ() == 0;
		}

// Full footprint coverage guarantees stability only when no boxes above can shift the combined CoM.
		BoxStackValue stackValue = placement.getStackValue();
		if(graph.getSupportees(placement).isEmpty() && graph.getSupportedArea(placement) == stackValue.getArea()) {
			return true;
		}

		int minSupportX = Integer.MAX_VALUE;
		int maxSupportX = Integer.MIN_VALUE;
		int minSupportY = Integer.MAX_VALUE;
		int maxSupportY = Integer.MIN_VALUE;

		for(PlacementLoad supporterLink : supporters) {
			Placement supporter = supporterLink.getPlacement();

			int overlapMinX = Math.max(placement.getAbsoluteX(), supporter.getAbsoluteX());
			int overlapMaxX = Math.min(placement.getAbsoluteEndX(), supporter.getAbsoluteEndX());
			int overlapMinY = Math.max(placement.getAbsoluteY(), supporter.getAbsoluteY());
			int overlapMaxY = Math.min(placement.getAbsoluteEndY(), supporter.getAbsoluteEndY());

			if(overlapMinX < minSupportX) minSupportX = overlapMinX;
			if(overlapMaxX > maxSupportX) maxSupportX = overlapMaxX;
			if(overlapMinY < minSupportY) minSupportY = overlapMinY;
			if(overlapMaxY > maxSupportY) maxSupportY = overlapMaxY;
		}

		// Accumulate weighted CoM of this box + the proportional share of every
		// box above it without rounding small support shares down to zero.
		double[] stack = accumulateStackCenterOfMass(graph, placement, 1.0);
		double totalWeight = stack[0];
		if(totalWeight == 0) {
			// A zero-weight stack has no weighted centre, so use this box's CoG.
			long centerOfGravity2X = CenterOfGravitySupportStabilityValidator.getCenterOfGravity2X(placement, stackValue);
			long centerOfGravity2Y = CenterOfGravitySupportStabilityValidator.getCenterOfGravity2Y(placement, stackValue);
			return centerOfGravity2X >= 2L * minSupportX && centerOfGravity2X <= 2L * maxSupportX
					&& centerOfGravity2Y >= 2L * minSupportY && centerOfGravity2Y <= 2L * maxSupportY;
		}

		// Coordinates remain doubled.
		double centerOfGravity2X = stack[1] / totalWeight;
		double centerOfGravity2Y = stack[2] / totalWeight;

		return centerOfGravity2X >= 2L * minSupportX && centerOfGravity2X <= 2L * maxSupportX
				&& centerOfGravity2Y >= 2L * minSupportY && centerOfGravity2Y <= 2L * maxSupportY;
	}
	
	protected static double[] accumulateStackCenterOfMass(SupportGraph graph, Placement placement, double share) {
		double[] mass = stackCenterOfMass(graph, placement, new IdentityHashMap<>());
		return new double[] { mass[0] * share, mass[1] * share, mass[2] * share };
	}

	/**
	 * The weight and the weighted doubled centre (x, y) of a placement plus its proportional share of
	 * every box above it. Each placement is calculated once, as one can be reached through several
	 * paths (and walking every path grows exponentially with the stack height).
	 */
	private static double[] stackCenterOfMass(SupportGraph graph, Placement placement, Map<Placement, double[]> masses) {
		double[] known = masses.get(placement);
		if(known != null) {
			return known;
		}
		BoxStackValue stackValue = placement.getStackValue();
		double w = placement.getWeight();
		long centerOfGravity2X = CenterOfGravitySupportStabilityValidator.getCenterOfGravity2X(placement, stackValue);
		long centerOfGravity2Y = CenterOfGravitySupportStabilityValidator.getCenterOfGravity2Y(placement, stackValue);

		double totalWeight  = w;
		double weightedComX = w * centerOfGravity2X;
		double weightedComY = w * centerOfGravity2Y;

		for(PlacementLoad supporteeLink : graph.getSupportees(placement)) {
			Placement supportee = supporteeLink.getPlacement();
			long supporteeArea = graph.getSupportedArea(supportee);
			if(supporteeArea == 0) {
				continue;
			}

			long overlapArea = placement.overlapArea2D(supportee);
			if(overlapArea == 0) {
				continue;
			}
			double supporteeShare = (double) overlapArea / supporteeArea;

			double[] sub = stackCenterOfMass(graph, supportee, masses);
			totalWeight  += sub[0] * supporteeShare;
			weightedComX += sub[1] * supporteeShare;
			weightedComY += sub[2] * supporteeShare;
		}

		double[] mass = new double[] { totalWeight, weightedComX, weightedComY };
		masses.put(placement, mass);
		return mass;
	}
}
