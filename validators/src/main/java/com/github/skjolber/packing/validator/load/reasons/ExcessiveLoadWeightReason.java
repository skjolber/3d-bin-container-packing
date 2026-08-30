package com.github.skjolber.packing.validator.load.reasons;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;

/**
 * Indicates that the accumulated load weight on top of a placement exceeds the
 * maximum permitted by {@link com.github.skjolber.packing.api.BoxStackValue#getMaxLoadWeight()}.
 */
public class ExcessiveLoadWeightReason implements ValidatorResultReason {

private static final int CODE = 14;

	private final Placement placement;
	private final long loadWeight;
	private final long maxLoadWeight;

	/**
	 * Creates a reason for a weight-load violation.
	 *
	 * @param placement the placement whose load weight was exceeded
	 * @param loadWeight the actual accumulated load weight above the placement
	 * @param maxLoadWeight the maximum load weight permitted by the stack value
	 */
	public ExcessiveLoadWeightReason(Placement placement, long loadWeight, long maxLoadWeight) {
		this.placement = placement;
		this.loadWeight = loadWeight;
		this.maxLoadWeight = maxLoadWeight;
	}

	/**
	 * Returns the placement whose max load weight was exceeded.
	 *
	 * @return the violating placement
	 */
	public Placement getPlacement() {
		return placement;
	}

	/**
	 * Returns the actual accumulated load weight above the placement.
	 *
	 * @return actual load weight
	 */
	public long getLoadWeight() {
		return loadWeight;
	}

	/**
	 * Returns the maximum load weight permitted for this placement's stack value.
	 *
	 * @return max permitted load weight
	 */
	public long getMaxLoadWeight() {
		return maxLoadWeight;
	}

	@Override
	public int getCode() {
		return CODE;
	}

	@Override
	public String getMessage() {
		return "Load weight " + loadWeight + " exceeds max " + maxLoadWeight + " for placement " + placement;
	}
}
