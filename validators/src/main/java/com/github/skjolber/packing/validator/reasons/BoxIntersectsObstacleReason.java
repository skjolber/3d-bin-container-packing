package com.github.skjolber.packing.validator.reasons;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;

/**
 * Indicates that a box takes up space which belongs to an obstacle: a box already in the container, see
 * {@link com.github.skjolber.packing.api.Container#getObstacles()}.
 */
public class BoxIntersectsObstacleReason implements ValidatorResultReason {

	private static final int CODE = 15;

	private final Placement placement;
	private final Placement obstacle;

	/**
	 * @param placement the placed box
	 * @param obstacle the obstacle it intersects
	 */
	public BoxIntersectsObstacleReason(Placement placement, Placement obstacle) {
		this.placement = placement;
		this.obstacle = obstacle;
	}

	/**
	 * @return the placed box
	 */
	public Placement getPlacement() {
		return placement;
	}

	/**
	 * @return the obstacle which the box intersects
	 */
	public Placement getObstacle() {
		return obstacle;
	}

	@Override
	public int getCode() {
		return CODE;
	}

	@Override
	public String getMessage() {
		return "Placement " + placement + " intersects the obstacle " + obstacle;
	}
}
