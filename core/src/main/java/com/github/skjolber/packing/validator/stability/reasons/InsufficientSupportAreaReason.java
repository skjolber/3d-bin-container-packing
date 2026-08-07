package com.github.skjolber.packing.validator.stability.reasons;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.stability.FullySupportedStabilityValidator;

/**
 * Indicates that a placement is not fully supported from below: its bottom footprint
 * is only partially covered by supporters and it does not rest on the container floor.
 *
 * @see FullySupportedStabilityValidator
 */
public class InsufficientSupportAreaReason implements ValidatorResultReason {

	private static final int CODE = 20;

	private final Placement placement;
	private final long supportedArea;
	private final long requiredArea;

	/**
	 * Creates a reason for an insufficient-support-area violation.
	 *
	 * @param placement the placement that is not fully supported
	 * @param supportedArea the actual area covered by supporters
	 * @param requiredArea the full bottom footprint area that must be covered
	 */
	public InsufficientSupportAreaReason(Placement placement, long supportedArea, long requiredArea) {
		this.placement = placement;
		this.supportedArea = supportedArea;
		this.requiredArea = requiredArea;
	}

	/**
	 * Returns the placement that is not fully supported.
	 *
	 * @return the violating placement
	 */
	public Placement getPlacement() {
		return placement;
	}

	/**
	 * Returns the area of the bottom footprint actually covered by supporters.
	 *
	 * @return supported area
	 */
	public long getSupportedArea() {
		return supportedArea;
	}

	/**
	 * Returns the full bottom footprint area that must be covered for full support.
	 *
	 * @return required area
	 */
	public long getRequiredArea() {
		return requiredArea;
	}

	@Override
	public int getCode() {
		return CODE;
	}

	@Override
	public String getMessage() {
		return "Placement " + placement + " supported area " + supportedArea + " is less than required " + requiredArea;
	}
}
