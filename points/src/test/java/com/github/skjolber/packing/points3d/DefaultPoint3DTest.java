package com.github.skjolber.packing.points3d;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;

public class DefaultPoint3DTest {

	private DefaultPoint3D point = new DefaultPoint3D(0, 0, 0, 9, 9, 9);

	@Test
	public void test() {
		assertFalse(point.isSupportedXYPlane());
		assertFalse(point.isSupportedXZPlane());
		assertFalse(point.isSupportedYZPlane());
	}

	@Test
	public void testCopy() {
		DefaultPoint3D copy = point.copy();
		
		assertEquals(point.getMinX(), copy.getMinX());
		assertEquals(point.getMinY(), copy.getMinY());
		assertEquals(point.getMinZ(), copy.getMinZ());
		assertEquals(point.getMaxX(), copy.getMaxX());
		assertEquals(point.getMaxY(), copy.getMaxY());
		assertEquals(point.getMaxZ(), copy.getMaxZ());
	}

	@Test
	public void testConstrainedEclipses() {
		DefaultPoint3D candidate = new DefaultPoint3D(0, 0, 0, 4, 4, 4);
		DefaultPoint3D largerX = new DefaultPoint3D(0, 0, 0, 9, 4, 4);
		DefaultPoint3D largerY = new DefaultPoint3D(0, 0, 0, 4, 9, 4);
		DefaultPoint3D largerZ = new DefaultPoint3D(0, 0, 0, 4, 4, 9);

		assertTrue(largerX.eclipsesConstrainedX(candidate, 4));
		assertFalse(candidate.eclipsesConstrainedX(largerX, 9));

		assertTrue(largerY.eclipsesConstrainedY(candidate, 4));
		assertFalse(candidate.eclipsesConstrainedY(largerY, 9));

		assertTrue(largerZ.eclipsesConstrainedZ(candidate, 4));
		assertFalse(candidate.eclipsesConstrainedZ(largerZ, 9));
	}
}
