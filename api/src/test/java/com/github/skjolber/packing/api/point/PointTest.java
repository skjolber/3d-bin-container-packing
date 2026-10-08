package com.github.skjolber.packing.api.point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PointTest {

	private static class TestPoint extends Point {

		TestPoint(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
		}

		@Override
		public Point copy(int maxX, int maxY, int maxZ) {
			return new TestPoint(minX, minY, minZ, maxX, maxY, maxZ);
		}

		@Override
		public boolean isSupportedXYPlane(int x, int y) {
			return false;
		}
	}

	@Test
	void setMaxZUpdatesDepthAndVolume() {
		Point point = new TestPoint(0, 0, 2, 9, 9, 9);

		point.setMaxZ(5);

		assertEquals(5, point.getMaxZ());
		assertEquals(4, point.getDz());
		assertEquals(10L * 10L * 4L, point.getVolume());
	}

	@Test
	void setMaxZDoesNotDependOnMaxX() {
		Point point = new TestPoint(0, 0, 0, -1, 9, 9);

		point.setMaxZ(4);

		assertEquals(4, point.getMaxZ());
		assertEquals(5, point.getDz());
	}

	@Test
	void setMaxZRejectsNegativeValues() {
		Point point = new TestPoint(1, 2, 3, 9, 9, 9);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> point.setMaxZ(-1));

		assertEquals("Cannot set max z to -1 for 1x2x3", exception.getMessage());
	}

	@Test
	void setMaxXAndYRejectNegativeValues() {
		Point point = new TestPoint(1, 2, 3, 9, 9, 9);

		assertThrows(RuntimeException.class, () -> point.setMaxX(-1));
		assertThrows(RuntimeException.class, () -> point.setMaxY(-1));
	}
}
