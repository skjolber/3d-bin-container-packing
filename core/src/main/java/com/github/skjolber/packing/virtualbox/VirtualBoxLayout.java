package com.github.skjolber.packing.virtualbox;

import java.util.Arrays;
import java.util.List;
import com.github.skjolber.packing.api.Placement;

/**
 * A rectangular envelope and its physical placements. Virtual boxes require filled
 * envelopes. Placement coordinates are
 * relative to the virtual box origin. The list, placements, original box items
 * and orientations must not be modified during use.
 */
public class VirtualBoxLayout {
	
	protected final VirtualBoxBounds bounds;
	protected final List<Placement> placements;
	protected volatile boolean prepared;

	/** Retain the arguments directly; callers must not modify them or their contents after construction. */
	public VirtualBoxLayout(VirtualBoxBounds bounds, List<Placement> placements) {
		this.bounds = bounds;
		this.placements = placements;
	}

	public VirtualBoxBounds getBounds() {
		return bounds;
	}

	public List<Placement> getPlacements() {
		return placements;
	}

	/**
	 * Validate a filled virtual-box layout once.
	 * No placement or load graph is modified. Generated grids use their known geometry
	 * instead of repeating general overlap checks.
	 */
	public void prepare() {
		if(!prepared) {
			synchronized(this) {
				if(!prepared) {
					prepareGeometry();
					prepared = true;
				}
			}
		}
	}

	protected void prepareGeometry() {
		VirtualBoxBounds.validateDimensions(bounds.dx(), bounds.dy(), bounds.dz());
		if(placements.isEmpty()) {
			throw new IllegalArgumentException("Expected a non-empty virtual layout");
		}
		long volume = 0;
		long sumDx = 0, sumDy = 0, sumDz = 0;
		for(Placement placement : placements) {
			var value = placement.getStackValue();
			if(value.getDx() <= 0 || value.getDy() <= 0 || value.getDz() <= 0
					|| placement.getAbsoluteX() < 0 || placement.getAbsoluteY() < 0 || placement.getAbsoluteZ() < 0
					|| (long) placement.getAbsoluteX() + value.getDx() > bounds.dx()
					|| (long) placement.getAbsoluteY() + value.getDy() > bounds.dy()
					|| (long) placement.getAbsoluteZ() + value.getDz() > bounds.dz()) {
				throw new IllegalArgumentException("Virtual layout placement is outside its bounds");
			}
			if(value.getVolume() > bounds.getVolume() - volume) {
				throw new IllegalArgumentException("Virtual layout volume exceeds its bounds");
			}
			volume += value.getVolume();
			sumDx += value.getDx();
			sumDy += value.getDy();
			sumDz += value.getDz();
		}
		if(volume != bounds.getVolume()) {
			throw new IllegalArgumentException("Expected a filled rectangular virtual layout");
		}
		// Sweep the axis with the most estimated divisions. In particular, a long
		// one-row grid is checked against its neighbours, not every other child.
		double divisions = (double) bounds.dx() / sumDx;
		int sweepAxis = 0;
		if((double) bounds.dy() / sumDy > divisions) {
			divisions = (double) bounds.dy() / sumDy;
			sweepAxis = 1;
		}
		if((double) bounds.dz() / sumDz > divisions) {
			sweepAxis = 2;
		}
		long[] placementOrder = new long[placements.size()];
		for(int i = 0; i < placementOrder.length; i++) {
			placementOrder[i] = ((long) minimum(placements.get(i), sweepAxis) << 32) | i;
		}
		Arrays.sort(placementOrder);
		for(int i = 0; i < placementOrder.length; i++) {
			Placement left = placements.get((int) placementOrder[i]);
			int end = maximum(left, sweepAxis);
			for(int j = i + 1; j < placementOrder.length && (placementOrder[j] >>> 32) <= end; j++) {
				if(left.intersects(placements.get((int) placementOrder[j]))) {
					throw new IllegalArgumentException("Virtual layout placements overlap");
				}
			}
		}
	}

	protected int minimum(Placement placement, int sweepAxis) {
		return sweepAxis == 0 ? placement.getAbsoluteX() : sweepAxis == 1 ? placement.getAbsoluteY() : placement.getAbsoluteZ();
	}

	protected int maximum(Placement placement, int sweepAxis) {
		return sweepAxis == 0 ? placement.getAbsoluteEndX() : sweepAxis == 1 ? placement.getAbsoluteEndY() : placement.getAbsoluteEndZ();
	}

}
