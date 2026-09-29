package com.github.skjolber.packing.virtualbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBounds;

/**
 * A rectangular envelope and its physical placements. Bounding-box search results
 * may contain empty space; virtual-box generators retain only filled envelopes. Placement coordinates are
 * relative to the virtual box origin. The list, placements, original box items
 * and orientations must not be modified during use.
 * Load-aware search results retain the complete support graph between placements.
 */
public class VirtualBoxLayout {
	
	protected final VirtualBoxBounds bounds;
	protected final List<Placement> placements;
	protected volatile boolean prepared;
	protected long minimumArea;
	protected long minimumVolume;
	protected int sweepAxis;
	protected long[] placementOrder;
	protected volatile VirtualBoxLayoutSupport loadSupport;

	/** Retain the arguments directly; callers must not modify them or their contents after construction. */
	public VirtualBoxLayout(VirtualBoxBounds bounds, List<Placement> placements) {
		this.bounds = bounds;
		this.placements = placements;
	}

	public VirtualBoxBounds getBoundingBox() {
		return bounds;
	}

	public List<Placement> getPlacements() {
		return placements;
	}

	/**
	 * Validate a filled virtual-box layout and cache its minima once. Bounding-box
	 * search results with empty space may still be represented by this class, but
	 * cannot be prepared as virtual boxes. No placement or load graph is modified.
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
		minimumArea = Long.MAX_VALUE;
		minimumVolume = Long.MAX_VALUE;
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
			minimumArea = Math.min(minimumArea, value.getArea());
			minimumVolume = Math.min(minimumVolume, value.getVolume());
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
		sweepAxis = 0;
		if((double) bounds.dy() / sumDy > divisions) {
			divisions = (double) bounds.dy() / sumDy;
			sweepAxis = 1;
		}
		if((double) bounds.dz() / sumDz > divisions) {
			sweepAxis = 2;
		}
		placementOrder = new long[placements.size()];
		for(int i = 0; i < placementOrder.length; i++) {
			placementOrder[i] = ((long) minimum(placements.get(i)) << 32) | i;
		}
		Arrays.sort(placementOrder);
		for(int i = 0; i < placementOrder.length; i++) {
			Placement left = placements.get((int) placementOrder[i]);
			int end = maximum(left);
			for(int j = i + 1; j < placementOrder.length && (placementOrder[j] >>> 32) <= end; j++) {
				if(left.intersects(placements.get((int) placementOrder[j]))) {
					throw new IllegalArgumentException("Virtual layout placements overlap");
				}
			}
		}
	}

	protected int minimum(Placement placement) {
		return sweepAxis == 0 ? placement.getAbsoluteX() : sweepAxis == 1 ? placement.getAbsoluteY() : placement.getAbsoluteZ();
	}

	protected int maximum(Placement placement) {
		return sweepAxis == 0 ? placement.getAbsoluteEndX() : sweepAxis == 1 ? placement.getAbsoluteEndY() : placement.getAbsoluteEndZ();
	}

	public long getMinimumArea() {
		prepare();
		return minimumArea;
	}

	public long getMinimumVolume() {
		prepare();
		return minimumVolume;
	}

	/** Prepare shared, read-only contact metadata only when load-aware packing needs it. */
	public VirtualBoxLayoutSupport getLoadSupport() {
		prepare();
		VirtualBoxLayoutSupport result = loadSupport;
		if(result == null) {
			synchronized(this) {
				result = loadSupport;
				if(result == null) {
					loadSupport = result = VirtualBoxLayoutSupport.create(this);
				}
			}
		}
		return result;
	}

	/** Create independent mutable placements for one worker/search frame, without copying template load links. */
	public List<Placement> createPlacements(boolean load) {
		List<Placement> result = new ArrayList<>(placements.size());
		for(Placement placement : placements) {
			result.add(new Placement(placement.getStackValue(), -1, placement.getAbsoluteX(), placement.getAbsoluteY(), placement.getAbsoluteZ(), load));
		}
		return result;
	}

	/**
	 * Translate a worker/frame's placements created by {@link #createPlacements(boolean)}.
	 * The translated envelope must fit the container. Do not call while a point
	 * calculator or load graph still retains these mutable placements; undo first.
	 * Original identities, orientations and list order are preserved.
	 */
	public void translate(int x, int y, int z, List<Placement> target) {
		if(target.size() != placements.size()) {
			throw new IllegalArgumentException("Expected one target per layout placement");
		}
		for(int i = 0; i < placements.size(); i++) {
			Placement relative = placements.get(i);
			target.get(i).setPoint(-1, x + relative.getAbsoluteX(), y + relative.getAbsoluteY(), z + relative.getAbsoluteZ());
		}
	}
}
