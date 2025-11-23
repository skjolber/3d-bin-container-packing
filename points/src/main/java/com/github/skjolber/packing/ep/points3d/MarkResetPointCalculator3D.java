package com.github.skjolber.packing.ep.points3d;

import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.ep.PlacementList;

public class MarkResetPointCalculator3D extends DefaultPointCalculator3D {

	protected long markMinVolumeLimit = 0;
	protected long markMinAreaLimit = 0;

	protected Point3DFlagList markValues = new Point3DFlagList(); // i.e. current (input) values

	protected PlacementList markPlacements;
	
	public MarkResetPointCalculator3D(boolean cloneOnConstrain, int capacity) {
		super(cloneOnConstrain, capacity);
		
		markPlacements = new PlacementList(capacity);
	}
	
	public MarkResetPointCalculator3D(boolean cloneOnConstrain, BoxItemSource source) {
		super(cloneOnConstrain, source);
		
		markPlacements = new PlacementList(placements.getCapacity());
	}

	public void mark() {
		this.markValues = values.clone(!immutablePoints);
		
		this.markMinAreaLimit = minAreaLimit;
		this.markMinVolumeLimit = minVolumeLimit;
		
		this.markPlacements = new PlacementList(placements);
	}
	
	public void reset() {
		this.minAreaLimit = markMinAreaLimit;
		this.minVolumeLimit = markMinVolumeLimit;
		
		this.values = markValues;
		this.placements = markPlacements;
		
		updateIndexes(markValues);
	}

}
