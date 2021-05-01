package com.github.skjolber.packing.api.packar.laff;

import java.util.List;

import com.github.skjolber.packing.api.Container;

public class LargestAreaFitFirstPackagerBuilder {

	private boolean footprintFirst = true;
	
	private List<Container> containers;
	
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
		return new LargestAreaFitFirstPackager(containers, footprintFirst, binarySearch, checkpointsPerDeadlineCheck);
	}
	
}
