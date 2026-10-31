package com.github.skjolber.packing.accelerator;

/**
 * Geometry of free points which are stored as {@link FlatLayout#POINT_INTS} consecutive ints in an {@code int[]}, addressed by the
 * offset of the first int. All coordinates are inclusive, as for {@link com.github.skjolber.packing.api.point.Point}.
 */
public final class FlatGeometry {

	public static final int MIN_X = 0;
	public static final int MIN_Y = 1;
	public static final int MIN_Z = 2;
	public static final int MAX_X = 3;
	public static final int MAX_Y = 4;
	public static final int MAX_Z = 5;
	public static final int PLANES = 6;

	private FlatGeometry() {
	}

	public static int dx(int[] s, int p) {
		return s[p + MAX_X] - s[p + MIN_X] + 1;
	}

	public static int dy(int[] s, int p) {
		return s[p + MAX_Y] - s[p + MIN_Y] + 1;
	}

	public static int dz(int[] s, int p) {
		return s[p + MAX_Z] - s[p + MIN_Z] + 1;
	}

	public static long area(int[] s, int p) {
		return (long)dx(s, p) * dy(s, p);
	}

	public static long volume(int[] s, int p) {
		return (long)dz(s, p) * dy(s, p) * dx(s, p);
	}

	public static boolean fits3D(int[] s, int p, int sx, int sy, int sz) {
		return !(sx > dx(s, p) || sy > dy(s, p) || sz > dz(s, p));
	}

	public static void copyPoint(int[] s, int from, int to) {
		s[to] = s[from];
		s[to + 1] = s[from + 1];
		s[to + 2] = s[from + 2];
		s[to + 3] = s[from + 3];
		s[to + 4] = s[from + 4];
		s[to + 5] = s[from + 5];
		s[to + 6] = s[from + 6];
	}

	/** @return true if the point a contains the point p, the same as {@code Point.eclipses} */
	public static boolean eclipses(int[] s, int a, int p) {
		return s[a + MIN_X] <= s[p + MIN_X] && s[p + MAX_X] <= s[a + MAX_X] && s[a + MIN_Y] <= s[p + MIN_Y] && s[p + MAX_Y] <= s[a + MAX_Y] && s[a + MIN_Z] <= s[p + MIN_Z]
				&& s[p + MAX_Z] <= s[a + MAX_Z];
	}

	private static boolean eclipsesX(int[] s, int a, int p) {
		return s[a + MIN_X] <= s[p + MIN_X] && s[p + MAX_X] <= s[a + MAX_X];
	}

	private static boolean eclipsesY(int[] s, int a, int p) {
		return s[a + MIN_Y] <= s[p + MIN_Y] && s[p + MAX_Y] <= s[a + MAX_Y];
	}

	private static boolean eclipsesZ(int[] s, int a, int p) {
		return s[a + MIN_Z] <= s[p + MIN_Z] && s[p + MAX_Z] <= s[a + MAX_Z];
	}

	/** @return true if the point a contains the point p moved to x, the same as {@code Point.eclipsesMovedX} */
	public static boolean eclipsesMovedX(int[] s, int a, int p, int x) {
		return s[a + MIN_X] <= x && s[p + MAX_X] <= s[a + MAX_X] && eclipsesY(s, a, p) && eclipsesZ(s, a, p);
	}

	public static boolean eclipsesMovedY(int[] s, int a, int p, int y) {
		return s[a + MIN_Y] <= y && s[p + MAX_Y] <= s[a + MAX_Y] && eclipsesX(s, a, p) && eclipsesZ(s, a, p);
	}

	public static boolean eclipsesMovedZ(int[] s, int a, int p, int z) {
		return s[a + MIN_Z] <= z && s[p + MAX_Z] <= s[a + MAX_Z] && eclipsesX(s, a, p) && eclipsesY(s, a, p);
	}

	public static long areaAtX(int[] s, int p, int xx) {
		return (long)dy(s, p) * (s[p + MAX_X] - xx + 1);
	}

	public static long areaAtY(int[] s, int p, int yy) {
		return (long)dx(s, p) * (s[p + MAX_Y] - yy + 1);
	}

	public static long areaAtMaxX(int[] s, int p, int maxX) {
		return (long)dy(s, p) * (maxX - s[p + MIN_X] + 1);
	}

	public static long areaAtMaxY(int[] s, int p, int maxY) {
		return (long)dx(s, p) * (maxY - s[p + MIN_Y] + 1);
	}

	public static long volumeAtZ(int[] s, int p, int zz) {
		return (long)dx(s, p) * dy(s, p) * (s[p + MAX_Z] - zz + 1);
	}

	public static long volumeAtMaxZ(int[] s, int p, int maxZ) {
		return (long)dx(s, p) * dy(s, p) * (maxZ - s[p + MIN_Z] + 1);
	}

	/** @return true if the point a is after the point b in the order of {@code Point.COMPARATOR_X_THEN_Y_THEN_Z} */
	public static boolean isAfterXyz(int[] s, int a, int b) {
		for (int i = 0; i < 6; i++) {
			int x = s[a + i];
			int y = s[b + i];
			if(x != y) {
				return x > y;
			}
		}
		return false;
	}
}
