package com.github.skjolber.packing.virtualbox;

/**
 * Dimensions of an origin-anchored rectangular envelope around a complete packing.
 * Equality of envelope and content volumes identifies a filled rectangular assembly.
 * Layout preparation validates dimensions once; construction is unchecked.
 */
public class VirtualBoxBounds {
	protected final int dx;
	protected final int dy;
	protected final int dz;

	/** Construct already validated, positive dimensions whose volume fits in a long. */
	public VirtualBoxBounds(int dx, int dy, int dz) {
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
	}

	/** Checked factory for callers outside a prepared layout. */
	public static VirtualBoxBounds of(int dx, int dy, int dz) {
		validateDimensions(dx, dy, dz);
		return new VirtualBoxBounds(dx, dy, dz);
	}

	public static void validateDimensions(int dx, int dy, int dz) {
		if(dx <= 0 || dy <= 0 || dz <= 0) {
			throw new IllegalArgumentException("Expected positive bounding box dimensions");
		}
		if((long) dx * dy > Long.MAX_VALUE / dz) {
			throw new IllegalArgumentException("Bounding box volume exceeds long range");
		}
	}

	public int dx() { return dx; }
	public int dy() { return dy; }
	public int dz() { return dz; }

	public long getVolume() {
		return (long) dx * dy * dz;
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof VirtualBoxBounds other && dx == other.dx && dy == other.dy && dz == other.dz;
	}

	@Override
	public int hashCode() {
		return (dx * 31 + dy) * 31 + dz;
	}

	@Override
	public String toString() {
		return "VirtualBoxBounds[dx=" + dx + ", dy=" + dy + ", dz=" + dz + "]";
	}
}
