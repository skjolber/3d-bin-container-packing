package com.github.skjolber.packing.validator.stability;

import java.util.List;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.StabilityValidator;
import com.github.skjolber.packing.validator.stability.reasons.InsufficientSupportAreaReason;

/**
 * Validates that every placement is fully supported from below: either it rests directly
 * on the container floor ({@code z == 0}), or the total area of its support contact
 * patches equals its entire bottom footprint area.
 *
 * <p>This is the strictest area-based stability criterion. For a more relaxed check,
 * use {@link PercentSupportedStabilityValidator} with a threshold below 100%.
 *
 * @see Placement#getSupportedArea()
 * @see BoxStackValue#getArea()
 */
public class FullySupportedStabilityValidator implements StabilityValidator {

	/**
	 * {@inheritDoc}
	 *
	 * <p>Adds an {@link InsufficientSupportAreaReason} for every placement that is not on the
	 * container floor and whose supported area is less than its full footprint area.
	 *
	 * @return {@code true} if every placement is fully supported; {@code false} otherwise
	 */
	@Override
	public boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons) {
		boolean valid = true;

		for(Placement placement : list) {
			if(placement.getAbsoluteZ() == 0) {
				// Resting on the container floor — unconditionally supported.
				continue;
			}

			long area = placement.getStackValue().getArea();
			long supportedArea = placement.getSupportedArea();

			if(supportedArea < area) {
				reasons.add(new InsufficientSupportAreaReason(placement, supportedArea, area));
				valid = false;
			}
		}

		return valid;
	}
}
