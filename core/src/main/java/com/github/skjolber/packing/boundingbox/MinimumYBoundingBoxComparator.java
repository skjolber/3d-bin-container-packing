package com.github.skjolber.packing.boundingbox;

/** Minimum Y extent, followed by the volume-first ordering. */
public class MinimumYBoundingBoxComparator extends MinimumVolumeBoundingBoxComparator {
	@Override
	public int compare(int dx, int dy, int dz, BoundingBox other) {
		int comparison = Integer.compare(dy, other.dy());
		return comparison != 0 ? comparison : super.compare(dx, dy, dz, other);
	}

	@Override
	public boolean canImprove(int dx, int dy, int dz, BoundingBox best) {
		return dy <= best.dy();
	}
}
