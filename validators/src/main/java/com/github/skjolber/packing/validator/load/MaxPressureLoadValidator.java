package com.github.skjolber.packing.validator.load;

import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadPressureReason;

/**
 * Validates that the load pressure on top of each placement does not exceed
 * the limit set by {@link BoxStackValue#getMaxLoadPressure()}.
 *
 * <p>Only placements for which {@link BoxStackValue#isMaxLoadPressure()} returns {@code true}
 * are checked. Pressure is expressed as {@code loadWeight × 1000 / area}, matching the
 * convention used by {@link com.github.skjolber.packing.api.Box#getMinimumPressure()}.
 *
 * <p>The load weight is computed independently by traversing the supportee graph — it does
 * not rely on the cached {@code loadWeight} field maintained by the placement graph.
 * Pressure is evaluated per supportee contact area ({@link com.github.skjolber.packing.api.PlacementLoad#getArea()}):
 * for each box resting directly on a placement, the proportional weight share (including
 * recursively propagated load from above) is divided by that box's contact area to derive
 * the pressure at that point. The maximum pressure across all supportee links is then
 * compared against the configured limit.
 *
 * <p>When both weight and pressure constraints are set on a stack value, this validator
 * checks only the pressure limit. The {@link WeightLoadValidator} checks the weight limit
 * independently.
 *
 * @see BoxStackValue#getMaxLoadPressure()
 * @see Placement#getSupportees()
 */
public class MaxPressureLoadValidator implements LoadValidator {

	/**
	 * {@inheritDoc}
	 *
	 * <p>Iterates all placements that declare a max load pressure and adds an
	 * {@link ExcessiveLoadPressureReason} for each one whose computed load pressure
	 * exceeds the permitted maximum.
	 *
	 * @return {@code true} if no pressure constraints are violated; {@code false} otherwise
	 */
	@Override
	public boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons) {
		boolean valid = true;

		for(Placement placement : list) {
			BoxStackValue stackValue = placement.getStackValue();

			if(!stackValue.isMaxLoadPressure()) {
				continue;
			}

			double maxLoadPressure = stackValue.getMaxLoadPressure();
			long maxPressure = 0;

			for(PlacementLoad pl : placement.getSupportees()) {
				long contactArea = pl.getArea();
				if(contactArea == 0) {
					continue;
				}

				Placement supportee = pl.getPlacement();
				long supporteeArea = supportee.getSupportedArea();
				long share = (supporteeArea > 0) ? (1000L * contactArea) / supporteeArea : 1000L;

				// weight attributed to this link (scaled by share/1000) including descendant load
				long weightScaled = (long) supportee.getWeight() * share + WeightLoadValidator.accumulateWeight(supportee, share);

				// weightScaled already carries a ×1000 share scale, so remove that
				// scale after calculating pressure.
				long linkPressure = Box.calculatePressure(contactArea, weightScaled) / 1000L;
				if(linkPressure > maxPressure) {
					maxPressure = linkPressure;
				}
			}

			if(maxPressure > maxLoadPressure) {
				reasons.add(new ExcessiveLoadPressureReason(placement, maxPressure, maxLoadPressure));
				valid = false;
			}
		}

		return valid;
	}
}
