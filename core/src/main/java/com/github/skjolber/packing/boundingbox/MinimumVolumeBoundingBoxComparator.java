package com.github.skjolber.packing.boundingbox;

/** Volume, then surface area, then height, depth and width. */
public class MinimumVolumeBoundingBoxComparator implements BoundingBoxComparator {
	@Override
	public int compare(BoundingBox left, BoundingBox right) {
		return compare(left.dx(), left.dy(), left.dz(), right);
	}

	@Override
	public int compare(int dx, int dy, int dz, BoundingBox other) {
		int comparison = Long.compare((long) dx * dy * dz, other.getVolume());
		if(comparison != 0) {
			return comparison;
		}
		comparison = Long.compare((long) dx * dy + (long) dx * dz + (long) dy * dz,
				(long) other.dx() * other.dy() + (long) other.dx() * other.dz() + (long) other.dy() * other.dz());
		if(comparison != 0) {
			return comparison;
		}
		comparison = Integer.compare(dz, other.dz());
		if(comparison != 0) {
			return comparison;
		}
		comparison = Integer.compare(dy, other.dy());
		return comparison != 0 ? comparison : Integer.compare(dx, other.dx());
	}

	@Override
	public boolean canImprove(int dx, int dy, int dz, BoundingBox best) {
		return (long) dx * dy * dz <= best.getVolume();
	}
}
