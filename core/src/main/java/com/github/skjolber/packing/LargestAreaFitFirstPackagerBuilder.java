package com.github.skjolber.packing;

import java.util.List;

public class LargestAreaFitFirstPackagerBuilder {

	private boolean footprintFirst = true;
	
	private List<Container> containers;
	
	private boolean rotate3D = true;
	
	private boolean binarySearch;

	private int checkpointsPerDeadlineCheck = 1;

	public LargestAreaFitFirstPackagerBuilder withHighestBoxFirst() {
		this.footprintFirst = false;
		return this;
	}

	public LargestAreaFitFirstPackagerBuilder withLargestFootprintBoxFirst() {
		this.footprintFirst = true;
		return this;
	}

	public LargestAreaFitFirstPackagerBuilder withContainers(List<Container> containers) {
		this.containers = containers;
		return this;
	}

	public LargestAreaFitFirstPackagerBuilder withRotate3D() {
		rotate3D = true;
		return this;
	}
	public LargestAreaFitFirstPackagerBuilder withRotate2D() {
		rotate3D = false;
		return this;
	}

	public LargestAreaFitFirstPackagerBuilder withBinarySearch() {
		this.binarySearch = true;
		return this;
	}

	public LargestAreaFitFirstPackagerBuilder withLinearSearch() {
		this.binarySearch = false;
		return this;
	}

	public LargestAreaFitFirstPackagerBuilder withCheckpointsPerDeadlineCheck(int n) {
		this.checkpointsPerDeadlineCheck = n;
		return this;
	}
	
	public LargestAreaFitFirstPackager build() {
		if(containers == null) {
			throw new IllegalStateException("Expected containers");
		}
		return new LargestAreaFitFirstPackager(containers, rotate3D, footprintFirst, binarySearch, checkpointsPerDeadlineCheck);
	}
	
}
