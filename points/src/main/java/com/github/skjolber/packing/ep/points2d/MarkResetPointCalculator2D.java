package com.github.skjolber.packing.ep.points2d;

import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.ep.PlacementList;

/**
 * Point calculator which can save its state ({@link #mark()}) and return to it ({@link #reset()}).
 */
public class MarkResetPointCalculator2D extends DefaultPointCalculator2D {

	protected long markMinAreaLimit = 0;

	protected Point2DFlagList markValues = new Point2DFlagList(); // i.e. current (input) values

	protected PlacementList markPlacements;

	public MarkResetPointCalculator2D(boolean immutablePoints, int capacity) {
		super(immutablePoints, capacity);
		markPlacements = new PlacementList(capacity);
	}

	public MarkResetPointCalculator2D(boolean immutablePoints, BoxItemSource boxItemSource) {
		super(immutablePoints, boxItemSource);
		markPlacements = new PlacementList(placements.getCapacity());
	}

	public void mark() {
		// mutable points are constrained in place, so the mark needs its own copies
		this.markValues = values.clone(!cloneOnConstrain);

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
