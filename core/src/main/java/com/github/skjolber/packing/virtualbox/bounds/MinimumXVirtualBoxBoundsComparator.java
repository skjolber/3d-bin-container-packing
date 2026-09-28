package com.github.skjolber.packing.virtualbox.bounds;

/** Minimum X extent, followed by the volume-first ordering. */
public class MinimumXVirtualBoxBoundsComparator extends MinimumVolumeVirtualBoxBoundsComparator {
	@Override
	public int compare(int dx, int dy, int dz, VirtualBoxBounds other) {
		int comparison = Integer.compare(dx, other.dx());
		return comparison != 0 ? comparison : super.compare(dx, dy, dz, other);
	}

	@Override
	public boolean canImprove(int dx, int dy, int dz, VirtualBoxBounds best) {
		return dx <= best.dx();
	}
}
