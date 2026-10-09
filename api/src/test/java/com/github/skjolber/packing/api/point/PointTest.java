package com.github.skjolber.packing.api.point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class PointTest {

	private static class TestPoint extends Point {

		public TestPoint(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
		}

		@Override
		public Point clone(int maxX, int maxY, int maxZ) {
			return new TestPoint(minX, minY, minZ, maxX, maxY, maxZ);
		}
	}

	@Test
	void testSetMaxZ() {
		Point point = new TestPoint(1, 2, 3, 5, 6, 20);

		point.setMaxZ(10);

		assertEquals(10, point.getMaxZ());
		assertEquals(8, point.getDz());
		assertEquals(5L * 5L * 8L, point.getVolume());
		assertEquals(25, point.getArea());
	}

	@Test
	void testSetMaxZRejectsNegativeValue() {
		Point point = new TestPoint(1, 2, 3, 5, 6, 20);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> point.setMaxZ(-1));
		assertEquals("Cannot set max z to -1 for 1x2x3", exception.getMessage());

		// unchanged
		assertEquals(20, point.getMaxZ());
		assertEquals(18, point.getDz());
	}

	@Test
	void testSetMaxXAndYRejectNegativeValues() {
		Point point = new TestPoint(1, 2, 3, 5, 6, 20);

		assertThrows(RuntimeException.class, () -> point.setMaxX(-1));
		assertThrows(RuntimeException.class, () -> point.setMaxY(-1));
	}
}
