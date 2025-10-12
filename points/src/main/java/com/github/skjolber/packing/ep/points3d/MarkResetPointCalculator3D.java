package com.github.skjolber.packing.ep.points3d;

import java.util.ArrayList;

import com.github.skjolber.packing.api.Placement;

public class MarkResetPointCalculator3D extends DefaultPointCalculator3D {

	protected long markMinVolumeLimit = 0;
	protected long markMinAreaLimit = 0;

	protected Point3DFlagList markValues = new Point3DFlagList(); // i.e. current (input) values

	protected ArrayList<Placement> markPlacements = new ArrayList<>();
	
	public MarkResetPointCalculator3D(boolean cloneOnConstrain) {
		super(cloneOnConstrain);
	}

	public MarkResetPointCalculator3D() {
		this(false);
	}
	
	public void mark() {
		this.markValues = values.clone(!immutablePoints);
		
		this.markMinAreaLimit = minAreaLimit;
		this.markMinVolumeLimit = minVolumeLimit;
		
		this.markPlacements = new ArrayList<>(placements);
	}
	
	public void reset() {
		this.minAreaLimit = markMinAreaLimit;
		this.minVolumeLimit = markMinVolumeLimit;
		
		this.values = markValues;
		this.placements = markPlacements;
		
		updateIndexes(markValues);
	}

}
