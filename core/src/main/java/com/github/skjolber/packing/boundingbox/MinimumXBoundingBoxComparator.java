package com.github.skjolber.packing.boundingbox;

/** Minimum X extent, followed by the volume-first ordering. */
public class MinimumXBoundingBoxComparator extends MinimumVolumeBoundingBoxComparator {
	@Override
	public int compare(int dx, int dy, int dz, BoundingBox other) {
		int comparison = Integer.compare(dx, other.dx());
		return comparison != 0 ? comparison : super.compare(dx, dy, dz, other);
	}

	@Override
	public boolean canImprove(int dx, int dy, int dz, BoundingBox best) {
		return dx <= best.dx();
	}
}
