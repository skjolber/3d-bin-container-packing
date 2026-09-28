package com.github.skjolber.packing.virtualbox;

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
}
