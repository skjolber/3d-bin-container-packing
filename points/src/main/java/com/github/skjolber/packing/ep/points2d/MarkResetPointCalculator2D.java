package com.github.skjolber.packing.ep.points2d;

import java.util.ArrayList;

import com.github.skjolber.packing.api.Placement;

public class MarkResetPointCalculator2D extends DefaultPointCalculator2D {

	protected long markMinAreaLimit = 0;

	protected Point2DFlagList markValues = new Point2DFlagList(); // i.e. current (input) values

	protected ArrayList<Placement> markPlacements = new ArrayList<>();
	
	public MarkResetPointCalculator2D(boolean cloneOnConstrain) {
		super(cloneOnConstrain);
	}

	public MarkResetPointCalculator2D() {
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
