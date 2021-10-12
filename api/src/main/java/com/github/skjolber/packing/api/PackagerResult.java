package com.github.skjolber.packing.api;

import java.util.List;

public class PackagerResult {

	protected final long duration;
	protected final List<Container> containers;
	
	public PackagerResult(List<Container> containers, long duration) {
		super();
		this.containers = containers;
		this.duration = duration;
	}

	public List<Container> getContainers() {
		return containers;
	}
	
	public long getDuration() {
		return duration;
	}
}
