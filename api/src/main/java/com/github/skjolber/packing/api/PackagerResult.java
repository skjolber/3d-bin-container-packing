package com.github.skjolber.packing.api;

import java.util.Collections;
import java.util.List;

/**
 * 
 * Packager result. If the packaging operation was unsuccessful, the container list is empty.
 * 
 */

public class PackagerResult {

	protected final long duration;
	protected final List<Container> containers;
	protected final boolean timeout;

	public PackagerResult(List<Container> containers, long duration, boolean timeout) {
		this.containers = containers != null ? containers : Collections.emptyList();
		this.duration = duration;
		this.timeout = timeout;
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

}
