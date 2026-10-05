package com.github.skjolber.packing.validator.reasons;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;

/**
 * Indicates that a box is in a later container than a box with a lower container priority value (see
 * {@link com.github.skjolber.packing.api.BoxItem#withContainerPriority(int)}).
 */
public class ContainerPriorityReason implements ValidatorResultReason {

	private static final int CODE = 32;

	private final Placement placement;
	private final int container;
	private final Placement earlier;
	private final int earlierContainer;

	/**
	 * @param placement the box in the later container, with the lower priority value
	 * @param container its container index
	 * @param earlier the box in the earlier container, with the higher priority value
	 * @param earlierContainer its container index
	 */
	public ContainerPriorityReason(Placement placement, int container, Placement earlier, int earlierContainer) {
		this.placement = placement;
		this.container = container;
		this.earlier = earlier;
		this.earlierContainer = earlierContainer;
	}

	/**
	 * @return the box in the later container, with the lower priority value
	 */
	public Placement getPlacement() {
		return placement;
	}

	public int getContainer() {
		return container;
	}

	/**
	 * @return the box in the earlier container, with the higher priority value
	 */
	public Placement getEarlier() {
		return earlier;
	}

	public int getEarlierContainer() {
		return earlierContainer;
	}

	@Override
	public int getCode() {
		return CODE;
	}

	@Override
	public String getMessage() {
		return "Placement " + placement + " with container priority " + placement.getBoxItem().getContainerPriority() + " is in container " + container
				+ ", after " + earlier + " with container priority " + earlier.getBoxItem().getContainerPriority() + " in container " + earlierContainer;
	}
}
