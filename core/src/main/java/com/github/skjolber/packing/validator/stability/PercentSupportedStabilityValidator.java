package com.github.skjolber.packing.validator.stability;

import java.util.List;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.StabilityValidator;
import com.github.skjolber.packing.validator.stability.reasons.InsufficientSupportAreaReason;

/**
 * Validates that the percentage of each placement's bottom footprint covered by
 * supporters meets or exceeds a configurable minimum threshold.
 *
 * <p>Placements resting on the container floor ({@code z == 0}) are always considered
 * 100% supported and are never flagged regardless of the threshold. For all other
 * placements the support percentage is computed as:
 * <pre>{@code
 *   actualPercent = (int)(supportedArea * 100L / footprintArea)
 * }</pre>
 *
 * <p>For full-support enforcement use {@link FullySupportedStabilityValidator}, which
 * is equivalent to constructing this validator with {@code minPercent = 100} but
 * produces a more descriptive reason type.
 *
 * @see Placement#getSupportedArea()
 * @see BoxStackValue#getArea()
 */
public class PercentSupportedStabilityValidator implements StabilityValidator {

	private final int minPercent;

	/**
	 * Creates a validator that requires at least {@code minPercent}% of the bottom
	 * footprint to be covered by supporters.
	 *
	 * @param minPercent minimum required support percentage, in the range [0, 100]
	 * @throws IllegalArgumentException if {@code minPercent} is not in [0, 100]
	 */
	public PercentSupportedStabilityValidator(int minPercent) {
		if(minPercent < 0 || minPercent > 100) {
			throw new IllegalArgumentException("minPercent must be in [0, 100], got: " + minPercent);
		}
		this.minPercent = minPercent;
	}

	/**
	 * Returns the minimum required support percentage configured for this validator.
	 *
	 * @return minimum support percentage (0–100)
	 */
	public int getMinPercent() {
		return minPercent;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Adds an {@link InsufficientSupportAreaReason} for every placement (above the
	 * container floor) whose support percentage falls below {@link #getMinPercent()}.
	 *
	 * @return {@code true} if every placement meets the minimum support percentage; {@code false} otherwise
	 */
	@Override
	public boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons) {
		boolean valid = true;

		for(Placement placement : list) {
			if(placement.getAbsoluteZ() == 0) {
				// Resting on the container floor — unconditionally 100% supported.
				continue;
			}

			long area = placement.getStackValue().getArea();
			long supportedArea = placement.getSupportedArea();

			int actualPercent = (area == 0) ? 100 : (int)(supportedArea * 100L / area);

			if(actualPercent < minPercent) {
				reasons.add(new InsufficientSupportAreaReason(placement, supportedArea, (area * minPercent) / 100));
				valid = false;
			}
		}

		return valid;
	}
}
