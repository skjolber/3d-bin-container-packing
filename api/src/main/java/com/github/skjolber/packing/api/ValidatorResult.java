package com.github.skjolber.packing.api;

import java.util.List;

/**
 * 
 * Packager result. If the packaging operation was unsuccessful, the container
 * list is empty.
 * 
 */

public class ValidatorResult {

	protected final long duration;
	protected final List<Container> containers;
	protected final boolean valid;

	public ValidatorResult(List<Container> containers, long duration, boolean valid) {
		this.containers = containers;
		this.duration = duration;
		this.valid = valid;
	}

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

	public boolean isValid() {
		return valid;
	}

}
