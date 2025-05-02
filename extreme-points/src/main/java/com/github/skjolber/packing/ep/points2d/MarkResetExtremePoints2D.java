package com.github.skjolber.packing.ep.points2d;

import java.util.ArrayList;

import com.github.skjolber.packing.api.StackPlacement;

public class MarkResetExtremePoints2D extends ExtremePoints2D {

	protected long markMinAreaLimit = 0;

	protected Point2DFlagList markValues = new Point2DFlagList(); // i.e. current (input) values

	protected ArrayList<StackPlacement> markPlacements = new ArrayList<>();
	
	public MarkResetExtremePoints2D(boolean cloneOnConstrain) {
		super(cloneOnConstrain);
	}

	public MarkResetExtremePoints2D() {
		this(false);
	}
	
	public void mark() {
		this.markValues = values.clone(cloneOnConstrain);
		
		this.markMinAreaLimit = minAreaLimit;
		
		this.markPlacements = new ArrayList<>(placements);
	}
	
	public void reset() {
		this.minAreaLimit = markMinAreaLimit;
		
		this.values = markValues;
		this.placements = markPlacements;
		
		updateIndexes(markValues);
	}
	
}
