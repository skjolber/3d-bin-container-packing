package com.github.skjolber.packing.api;

import java.util.Collections;
import java.util.List;

/**
 * 
 * Packager result. If the packaging operation was unsuccessful, the container
 * list is empty.
 * 
 */

public class PackagerResult {

	protected final long duration;
	protected final List<Container> containers;
	protected final boolean timeout;
	protected final long cost;
	protected final boolean insertionOrder;

	public PackagerResult(List<Container> containers, long duration, boolean timeout) {
		this(containers, duration, timeout, -1);
	}

	/**
	 * A result which is in insertion order only if it has no containers.
	 */
	public PackagerResult(List<Container> containers, long duration, boolean timeout, long cost) {
		this(containers, duration, timeout, cost, containers == null || containers.isEmpty());
	}

	/**
	 * @param insertionOrder whether the placements of every container are known to be in insertion order
	 */
	public PackagerResult(List<Container> containers, long duration, boolean timeout, long cost, boolean insertionOrder) {
		this.containers = containers != null ? containers : Collections.emptyList();
		this.duration = duration;
		this.timeout = timeout;
		this.cost = cost;
		this.insertionOrder = insertionOrder;
	}

	/**
	 * Whether the placements of every container are in insertion order (see {@link ContainerAccess}): an order in which
	 * the boxes can be loaded. The packagers put results in insertion order unless that is skipped
	 * ({@link PackagerResultBuilder#withInsertionOrder(boolean)}); with a box item order, they only place boxes which
	 * can be inserted. False if skipped, or if the boxes cannot be loaded in any order (possible through a door,
	 * {@link ContainerAccess#FRONT}, as the packagers place boxes without regard to the door when there is no box item
	 * order).
	 *
	 * @return true if the placements are known to be in insertion order
	 */
	public boolean isInsertionOrder() {
		return insertionOrder;
	}

	/**
	 * Get list of containers necessary for the targeted packaging.
	 * 
	 * @return non-empty list if packaging was successful, otherwise an empty list.
	 */

	public List<Container> getContainers() {
		return containers;
	}

	public long getDuration() {
		return duration;
	}

	public Container get(int index) {
		return containers.get(index);
	}

	public int size() {
		return containers.size();
	}

	public boolean isSuccess() {
		return !containers.isEmpty();
	}

	public boolean isTimeout() {
		return timeout;
	}

	public long getCost() {
		return cost;
	}

}
