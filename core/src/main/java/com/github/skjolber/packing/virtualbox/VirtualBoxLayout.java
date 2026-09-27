package com.github.skjolber.packing.virtualbox;

import java.util.List;
import com.github.skjolber.packing.boundingbox.BoundingBox;

/**
 * A filled rectangular assembly. Coordinates and the placement list are immutable;
 * referenced original box items and orientations must not be modified during use.
 * Layouts are constructed by the grid and brute-force generators.
 */
public class VirtualBoxLayout {
	protected final BoundingBox bounds;
	protected final List<VirtualBoxPlacement> placements;

	protected VirtualBoxLayout(BoundingBox bounds, List<VirtualBoxPlacement> placements) {
		this.bounds = bounds;
		this.placements = List.copyOf(placements);
		// Generators already guarantee bounds, non-overlap and a filled envelope.
	}

	public BoundingBox getBoundingBox() {
		return bounds;
	}

	public List<VirtualBoxPlacement> getPlacements() {
		return placements;
	}
}
