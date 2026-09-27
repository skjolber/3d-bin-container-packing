package com.github.skjolber.packing.boundingbox;

/** Minimum Z extent, followed by the volume-first ordering. */
public class MinimumZBoundingBoxComparator extends MinimumVolumeBoundingBoxComparator {
	@Override
	public int compare(int dx, int dy, int dz, BoundingBox other) {
		int comparison = Integer.compare(dz, other.dz());
		return comparison != 0 ? comparison : super.compare(dx, dy, dz, other);
	}

	@Override
	public boolean canImprove(int dx, int dy, int dz, BoundingBox best) {
		return dz <= best.dz();
	}
}
