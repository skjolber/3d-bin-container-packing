package com.github.skjolber.packing.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PlacementTest {

	private static Placement placement(int dx, int dy, int dz, int x, int y, int z) {
		Box box = Box.newBuilder().withSize(dx, dy, dz).withWeight(1).build();
		BoxStackValue stackValue = box.getStackValue(0);

		// the unrotated stack value is expected first
		assertEquals(dx, stackValue.getDx());
		assertEquals(dy, stackValue.getDy());
		assertEquals(dz, stackValue.getDz());

		return new Placement(stackValue, 0, x, y, z);
	}

	/**
	 * Two bars crossing each other; neither bar has a start or end coordinate inside the other bar's range on the axis along which the other bar is longer.
	 * 
	 * <pre>
	 *  y\x 0 1 2 3 4 5 6 7 8 9
	 *   9  . . . B B . . . . .
	 *   8  . . . B B . . . . .
	 *   7  . . . B B . . . . .
	 *   6  . . . B B . . . . .
	 *   5  . . . B B . . . . .
	 *   4  A A A X X A A A A A
	 *   3  A A A X X A A A A A
	 *   2  . . . B B . . . . .
	 *   1  . . . B B . . . . .
	 *   0  . . . B B . . . . .
	 * </pre>
	 * 
	 * A is 10x2x1 at 0x3x0, B is 2x10x1 at 3x0x0 and X marks the overlap.
	 */

	@Test
	void testCrossingBarsIntersectInBothDirections() {
		Placement bar1 = placement(10, 2, 1, 0, 3, 0);
		Placement bar2 = placement(2, 10, 1, 3, 0, 0);

		// each bar strictly spans the other on a different axis
		assertTrue(bar1.intersectsX(bar2));
		assertTrue(bar2.intersectsX(bar1));
		assertTrue(bar1.intersectsY(bar2));
		assertTrue(bar2.intersectsY(bar1));
		assertTrue(bar1.intersectsZ(bar2));
		assertTrue(bar2.intersectsZ(bar1));

		assertTrue(bar1.intersects(bar2));
		assertTrue(bar2.intersects(bar1));

		assertTrue(bar1.intersects3D(bar2));
		assertTrue(bar2.intersects3D(bar1));
	}

	@Test
	void testContainedPlacementIntersectsInBothDirections() {
		Placement outer = placement(10, 10, 10, 0, 0, 0);
		Placement inner = placement(2, 2, 2, 4, 4, 4);

		assertTrue(outer.intersects(inner));
		assertTrue(inner.intersects(outer));

		assertTrue(outer.intersectsX(inner));
		assertTrue(inner.intersectsX(outer));
		assertTrue(outer.intersectsY(inner));
		assertTrue(inner.intersectsY(outer));
		assertTrue(outer.intersectsZ(inner));
		assertTrue(inner.intersectsZ(outer));

		// identical placements
		assertTrue(outer.intersects(placement(10, 10, 10, 0, 0, 0)));
	}

	@Test
	void testPlacementsApartDoNotIntersect() {
		Placement a = placement(2, 2, 2, 0, 0, 0);

		// directly adjacent on a single axis, i.e. sharing a face but no volume
		Placement besideX = placement(2, 2, 2, 2, 0, 0);
		assertFalse(a.intersectsX(besideX));
		assertFalse(besideX.intersectsX(a));
		assertFalse(a.intersects(besideX));
		assertFalse(besideX.intersects(a));

		Placement besideY = placement(2, 2, 2, 0, 2, 0);
		assertFalse(a.intersectsY(besideY));
		assertFalse(besideY.intersectsY(a));
		assertFalse(a.intersects(besideY));
		assertFalse(besideY.intersects(a));

		Placement above = placement(2, 2, 2, 0, 0, 2);
		assertFalse(a.intersectsZ(above));
		assertFalse(above.intersectsZ(a));
		assertFalse(a.intersects(above));
		assertFalse(above.intersects(a));

		// far apart
		Placement far = placement(2, 2, 2, 5, 5, 5);
		assertFalse(a.intersects(far));
		assertFalse(far.intersects(a));

		// the crossing bars, but with one on top of the other
		Placement bar1 = placement(10, 2, 1, 0, 3, 0);
		Placement bar2OnTop = placement(2, 10, 1, 3, 0, 1);
		assertTrue(bar1.intersectsX(bar2OnTop));
		assertTrue(bar1.intersectsY(bar2OnTop));
		assertFalse(bar1.intersectsZ(bar2OnTop));
		assertFalse(bar1.intersects(bar2OnTop));
		assertFalse(bar2OnTop.intersects(bar1));
	}

	@Test
	void testSingleSharedCellIntersects() {
		// coordinates are inclusive, so these share the cell at x=1
		Placement a = placement(2, 2, 2, 0, 0, 0);
		Placement b = placement(2, 2, 2, 1, 1, 1);

		assertTrue(a.intersects(b));
		assertTrue(b.intersects(a));
	}

	@Test
	void testAxisMethodsAgreeWithIntersects3DForAllPlacements() {
		Placement fixed = placement(3, 2, 2, 1, 1, 0);

		int count = 0;
		for (int dx = 1; dx <= 3; dx++) {
			for (int dy = 1; dy <= 3; dy++) {
				for (int dz = 1; dz <= 3; dz++) {
					for (int x = 0; x <= 5; x++) {
						for (int y = 0; y <= 5; y++) {
							for (int z = 0; z <= 5; z++) {
								Placement other = placement(dx, dy, dz, x, y, z);

								boolean expected = fixed.intersects3D(other);

								assertEquals(expected, fixed.intersects(other), fixed + " vs " + other);
								assertEquals(expected, other.intersects(fixed), other + " vs " + fixed);

								// axis methods are symmetric
								assertEquals(fixed.intersectsX(other), other.intersectsX(fixed), fixed + " vs " + other);
								assertEquals(fixed.intersectsY(other), other.intersectsY(fixed), fixed + " vs " + other);
								assertEquals(fixed.intersectsZ(other), other.intersectsZ(fixed), fixed + " vs " + other);
								count++;
							}
						}
					}
				}
			}
		}
		assertEquals(27 * 216, count);
	}
}
