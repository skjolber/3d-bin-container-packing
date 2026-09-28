package com.github.skjolber.packing.virtualbox.bounds;

/** Minimum Y extent, followed by the volume-first ordering. */
public class MinimumYVirtualBoxBoundsComparator extends MinimumVolumeVirtualBoxBoundsComparator {
	@Override
	public int compare(int dx, int dy, int dz, VirtualBoxBounds other) {
		int comparison = Integer.compare(dy, other.dy());
		return comparison != 0 ? comparison : super.compare(dx, dy, dz, other);
	}

	@Override
	public boolean canImprove(int dx, int dy, int dz, VirtualBoxBounds best) {
		return dy <= best.dy();
	}
}
