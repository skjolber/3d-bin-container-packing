package com.github.skjolber.packing.virtualbox.bounds;

/** Minimum Z extent, followed by the volume-first ordering. */
public class MinimumZVirtualBoxBoundsComparator extends MinimumVolumeVirtualBoxBoundsComparator {
	@Override
	public int compare(int dx, int dy, int dz, VirtualBoxBounds other) {
		int comparison = Integer.compare(dz, other.dz());
		return comparison != 0 ? comparison : super.compare(dx, dy, dz, other);
	}

	@Override
	public boolean canImprove(int dx, int dy, int dz, VirtualBoxBounds best) {
		return dz <= best.dz();
	}
}
