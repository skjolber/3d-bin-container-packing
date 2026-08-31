package com.github.skjolber.packing.validator.load.reasons;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;

/**
 * Indicates that the number of box levels stacked on top of a placement exceeds the
 * maximum permitted by {@link com.github.skjolber.packing.api.BoxStackValue#getMaxLoadBoxCount()}.
 */
public class ExcessiveLoadBoxCountReason implements ValidatorResultReason {

	private static final int CODE = 12;

	private final Placement placement;
	private final int boxCount;
	private final int maxLoadBoxCount;

	/**
	 * Creates a reason for a box-count-load violation.
	 *
	 * @param placement the placement whose max load box count was exceeded
	 * @param boxCount the actual number of box levels above the placement
	 * @param maxLoadBoxCount the maximum number of box levels permitted by the stack value
	 */
	public ExcessiveLoadBoxCountReason(Placement placement, int boxCount, int maxLoadBoxCount) {
		this.placement = placement;
		this.boxCount = boxCount;
		this.maxLoadBoxCount = maxLoadBoxCount;
	}

	/**
	 * Returns the placement whose max load box count was exceeded.
	 *
	 * @return the violating placement
	 */
	public Placement getPlacement() {
		return placement;
	}

	/**
	 * Returns the actual number of box levels stacked above the placement.
	 *
	 * @return actual box levels above the placement
	 */
	public int getBoxCount() {
		return boxCount;
	}

	/**
	 * Returns the maximum number of box levels permitted for this placement's stack value.
	 *
	 * @return max permitted box levels
	 */
	public int getMaxLoadBoxCount() {
		return maxLoadBoxCount;
	}

	@Override
	public int getCode() {
		return CODE;
	}

	@Override
	public String getMessage() {
		return "Load box count " + boxCount + " exceeds max " + maxLoadBoxCount + " for placement " + placement;
	}
}
