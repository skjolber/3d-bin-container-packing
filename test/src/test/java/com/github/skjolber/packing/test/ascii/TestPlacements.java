package com.github.skjolber.packing.test.ascii;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;

/**
 * Boxes at a position, with the given size (x, y and z) and no rotation.
 */
final class TestPlacements {

	private TestPlacements() {
		// utility class
	}

	/**
	 * @param id the id of the box
	 * @param dx size along x
	 * @param dy size along y
	 * @param dz size along z
	 * @param x position
	 * @param y position
	 * @param z position
	 * @return a placement of a box with the given id
	 */
	static Placement place(String id, int dx, int dy, int dz, int x, int y, int z) {
		return place(Box.newBuilder().withId(id), dx, dy, dz, x, y, z);
	}

	/**
	 * @param x minimum x
	 * @param y minimum y
	 * @param z minimum z
	 * @return a point at the given position, with a maximum corner at the same position
	 */
	static Point point(int x, int y, int z) {
		return point(x, y, z, x, y, z);
	}

	static Point point(int x, int y, int z, int maxX, int maxY, int maxZ) {
		return new Point(x, y, z, maxX, maxY, maxZ) {

			@Override
			public Point copy(int maxX, int maxY, int maxZ) {
				return point(getMinX(), getMinY(), getMinZ(), maxX, maxY, maxZ);
			}

			@Override
			public boolean isSupportedXYPlane(int x, int y) {
				return false;
			}
		};
	}

	static Placement place(Box.Builder builder, int dx, int dy, int dz, int x, int y, int z) {
		Box box = builder
				.withSize(dx, dy, dz)
				.withWeight(1)
				.build();

		BoxStackValue stackValue = box.getStackValue(0);
		if (stackValue.getDx() != dx || stackValue.getDy() != dy || stackValue.getDz() != dz) {
			throw new IllegalStateException("Expected first rotation of " + dx + "x" + dy + "x" + dz + ", got " + stackValue);
		}
		return new Placement(stackValue, 0, x, y, z);
	}
}
