package com.github.skjolber.packing.validator.load;

import java.util.List;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
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
 * The same proportional distribution as {@link WeightLoadValidator#accumulateWeight} is used;
 * pressure is then derived from the computed weight and the placement's top surface area.
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

			long area = stackValue.getArea();
			if(area == 0) {
				continue;
			}

			long loadWeight = WeightLoadValidator.accumulateWeight(placement, 1000L) / 1000L;
			long loadPressure = (loadWeight * 1000L) / area;
			double maxLoadPressure = stackValue.getMaxLoadPressure();

			if(loadPressure > maxLoadPressure) {
				reasons.add(new ExcessiveLoadPressureReason(placement, loadPressure, maxLoadPressure));
				valid = false;
			}
		}

		return valid;
	}
}
