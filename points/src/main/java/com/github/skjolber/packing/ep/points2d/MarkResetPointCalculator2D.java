package com.github.skjolber.packing.ep.points2d;

import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.ep.PlacementList;

public class MarkResetPointCalculator2D extends DefaultPointCalculator2D {

	protected long markMinAreaLimit = 0;

	protected Point2DFlagList markValues = new Point2DFlagList(); // i.e. current (input) values

	protected PlacementList markPlacements;
	
	public MarkResetPointCalculator2D(boolean cloneOnConstrain, int capacity) {
		super(cloneOnConstrain, capacity);
	}
	
	public MarkResetPointCalculator2D(boolean immutablePoints, BoxItemSource boxItemSource) {
		super(immutablePoints, boxItemSource);
	}

	public void mark() {
		this.markValues = values.clone(cloneOnConstrain);
		
		this.markMinAreaLimit = minAreaLimit;
		
		this.markPlacements = new PlacementList(placements);
	}
	
	public void reset() {
		this.minAreaLimit = markMinAreaLimit;
		
		this.values = markValues;
		this.placements = markPlacements;
		
		updateIndexes(markValues);
	}
	
}
