package com.github.skjolber.packing.validator.load.reasons;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;

/**
 * Indicates that the load pressure on top of a placement exceeds the maximum
 * permitted by {@link com.github.skjolber.packing.api.BoxStackValue#getMaxLoadPressure()}.
 *
 * <p>Pressure is expressed as {@code loadWeight × 1000 / area}, matching the
 * convention used by {@link com.github.skjolber.packing.api.Box#getMinimumPressure()}.
 */
public class ExcessiveLoadPressureReason implements ValidatorResultReason {

	private static final int CODE = 11;

	private final Placement placement;
	private final long loadPressure;
	private final double maxLoadPressure;

	/**
	 * Creates a reason for a pressure-load violation.
	 *
	 * @param placement the placement whose load pressure was exceeded
	 * @param loadPressure the actual load pressure above the placement
	 * @param maxLoadPressure the maximum load pressure permitted by the stack value
	 */
	public ExcessiveLoadPressureReason(Placement placement, long loadPressure, double maxLoadPressure) {
		this.placement = placement;
		this.loadPressure = loadPressure;
		this.maxLoadPressure = maxLoadPressure;
	}

	/**
	 * Returns the placement whose max load pressure was exceeded.
	 *
	 * @return the violating placement
	 */
	public Placement getPlacement() {
		return placement;
	}

	/**
	 * Returns the actual load pressure above the placement.
	 *
	 * @return actual load pressure ({@code loadWeight × 1000 / area})
	 */
	public long getLoadPressure() {
		return loadPressure;
	}

	/**
	 * Returns the maximum load pressure permitted for this placement's stack value.
	 *
	 * @return max permitted load pressure
	 */
	public double getMaxLoadPressure() {
		return maxLoadPressure;
	}

	@Override
	public int getCode() {
		return CODE;
	}

	@Override
	public String getMessage() {
		return "Load pressure " + loadPressure + " exceeds max " + maxLoadPressure + " for placement " + placement;
	}
}
