package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.LoadBearingConstraint;
import com.github.skjolber.packing.api.StabilityConstraint;
import com.github.skjolber.packing.api.StackValue;

public class BoundedStackValue {

	protected final StackValue values;
	protected final StabilityConstraint stabilityConstraints;
	protected final LoadBearingConstraint loadBearingConstraint;
	protected final int index;
	
	public BoundedStackValue(StackValue values, StabilityConstraint stabilityConstraints,
			LoadBearingConstraint loadBearingConstraint, int index) {
		super();
		this.values = values;
		this.stabilityConstraints = stabilityConstraints;
		this.loadBearingConstraint = loadBearingConstraint;
		this.index = index;
	}

	public StackValue getValues() {
		return values;
	}

	public StabilityConstraint getStabilityConstraints() {
		return stabilityConstraints;
	}

	public LoadBearingConstraint getLoadBearingConstraint() {
		return loadBearingConstraint;
	}

	public int getIndex() {
		return index;
	}
	
}
