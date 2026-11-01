package com.github.skjolber.packing.accelerator;

import static com.github.skjolber.packing.accelerator.FlatGeometry.*;

/**
 * The canonical total orders of the points which are moved past a placement, in the x, y and z direction. These are the same orders as
 * {@code CustomIntXComparator}, {@code CustomIntYComparator} and {@code CustomIntZComparator}; the order decides which of the moved points
 * which eclipse each other remain.
 * <p>
 * The planes of a point are placement ids; the end coordinates of the placements are found at the placement offset (the offset of the
 * placement table in the scratch).
 */
public final class FlatPointOrder {

	private FlatPointOrder() {
	}

	/** @return the supporting planes of a point: xy 4, xz 2, yz 1 */
	public static int supports(int planes) {
		return (FlatLayout.xy(planes) != FlatLayout.PLANE_NONE ? 4 : 0) + (FlatLayout.xz(planes) != FlatLayout.PLANE_NONE ? 2 : 0)
				+ (FlatLayout.yz(planes) != FlatLayout.PLANE_NONE ? 1 : 0);
	}

	/** @return the supporting planes which a point keeps when it is moved to xx: xy 4 and xz 2 */
	public static int supportsMovedX(int[] s, int placementOffset, int planes, int xx) {
		int xy = FlatLayout.xy(planes);
		int xz = FlatLayout.xz(planes);
		return (xy != FlatLayout.PLANE_NONE && xx <= s[placementOffset + xy * FlatLayout.PLACEMENT_INTS + 3] ? 4 : 0)
				+ (xz != FlatLayout.PLANE_NONE && xx <= s[placementOffset + xz * FlatLayout.PLACEMENT_INTS + 3] ? 2 : 0);
	}

	/** @return the supporting planes which a point keeps when it is moved to yy: xy 4 and yz 1 */
	public static int supportsMovedY(int[] s, int placementOffset, int planes, int yy) {
		int xy = FlatLayout.xy(planes);
		int yz = FlatLayout.yz(planes);
		return (xy != FlatLayout.PLANE_NONE && yy <= s[placementOffset + xy * FlatLayout.PLACEMENT_INTS + 4] ? 4 : 0)
				+ (yz != FlatLayout.PLANE_NONE && yy <= s[placementOffset + yz * FlatLayout.PLACEMENT_INTS + 4] ? 1 : 0);
	}

	/** @return the supporting planes which a point keeps when it is moved to zz: xz 2 and yz 1 */
	public static int supportsMovedZ(int[] s, int placementOffset, int planes, int zz) {
		int xz = FlatLayout.xz(planes);
		int yz = FlatLayout.yz(planes);
		return (xz != FlatLayout.PLANE_NONE && zz <= s[placementOffset + xz * FlatLayout.PLACEMENT_INTS + 5] ? 2 : 0)
				+ (yz != FlatLayout.PLANE_NONE && zz <= s[placementOffset + yz * FlatLayout.PLACEMENT_INTS + 5] ? 1 : 0);
	}

	/** @return true if the point a is after the point b in the order of {@code CustomIntXComparator}, for the points moved to xx */
	public static boolean isAfterX(int[] s, int placementOffset, int a, int b, int xx) {
		int ay = s[a + MIN_Y];
		int by = s[b + MIN_Y];
		if(ay != by) {
			return ay > by;
		}
		int az = s[a + MIN_Z];
		int bz = s[b + MIN_Z];
		if(az != bz) {
			return az > bz;
		}

		long volumeA = (long)dy(s, a) * dz(s, a) * s[a + MAX_X];
		long volumeB = (long)dy(s, b) * dz(s, b) * s[b + MAX_X];
		if(volumeA != volumeB) {
			return volumeA < volumeB;
		}

		return isAfterTied(s, placementOffset, a, b, 0, xx);
	}

	/** @return true if the point a is after the point b in the order of {@code CustomIntYComparator}, for the points moved to yy */
	public static boolean isAfterY(int[] s, int placementOffset, int a, int b, int yy) {
		int az = s[a + MIN_Z];
		int bz = s[b + MIN_Z];
		if(az != bz) {
			return az > bz;
		}
		int ax = s[a + MIN_X];
		int bx = s[b + MIN_X];
		if(ax != bx) {
			return ax > bx;
		}

		long volumeA = (long)dx(s, a) * dz(s, a) * s[a + MAX_Y];
		long volumeB = (long)dx(s, b) * dz(s, b) * s[b + MAX_Y];
		if(volumeA != volumeB) {
			return volumeA < volumeB;
		}

		return isAfterTied(s, placementOffset, a, b, 1, yy);
	}

	/** @return true if the point a is after the point b in the order of {@code CustomIntZComparator}, for the points moved to zz */
	public static boolean isAfterZ(int[] s, int placementOffset, int a, int b, int zz) {
		int ax = s[a + MIN_X];
		int bx = s[b + MIN_X];
		if(ax != bx) {
			return ax > bx;
		}
		int ay = s[a + MIN_Y];
		int by = s[b + MIN_Y];
		if(ay != by) {
			return ay > by;
		}

		long volumeA = area(s, a) * s[a + MAX_Z];
		long volumeB = area(s, b) * s[b + MAX_Z];
		if(volumeA != volumeB) {
			return volumeA < volumeB;
		}

		return isAfterTied(s, placementOffset, a, b, 2, zz);
	}

	/** The keys after the volume proxy, for the direction 0 (x), 1 (y) or 2 (z). */
	private static boolean isAfterTied(int[] s, int placementOffset, int a, int b, int direction, int moved) {
		for (int i = MAX_X; i <= MAX_Z; i++) {
			int x = s[a + i];
			int y = s[b + i];
			if(x != y) {
				return x < y;
			}
		}

		int supportsA;
		int supportsB;
		if(direction == 0) {
			supportsA = supportsMovedX(s, placementOffset, s[a + PLANES], moved);
			supportsB = supportsMovedX(s, placementOffset, s[b + PLANES], moved);
		} else if(direction == 1) {
			supportsA = supportsMovedY(s, placementOffset, s[a + PLANES], moved);
			supportsB = supportsMovedY(s, placementOffset, s[b + PLANES], moved);
		} else {
			supportsA = supportsMovedZ(s, placementOffset, s[a + PLANES], moved);
			supportsB = supportsMovedZ(s, placementOffset, s[b + PLANES], moved);
		}
		if(supportsA != supportsB) {
			return supportsA < supportsB;
		}

		if(s[a + direction] != s[b + direction]) {
			return s[a + direction] > s[b + direction];
		}
		supportsA = supports(s[a + PLANES]);
		supportsB = supports(s[b + PLANES]);
		return supportsA < supportsB;
	}
}
