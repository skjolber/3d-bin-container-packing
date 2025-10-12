package com.github.skjolber.packing.points3d;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.DefaultXZPlanePoint3D;

public class DefaultPoint3DTest {

	private DefaultPoint3D point = new DefaultPoint3D(0, 0, 0, 9, 9, 9);

	@Test
	public void test() {
		assertFalse(point.isSupportedXYPlane());
		assertFalse(point.isSupportedXZPlane());
		assertFalse(point.isSupportedYZPlane());
	}

	@Test
	public void testRotate() {
		DefaultPoint3D rotate = point.rotate().rotate().rotate();
		assertEquals(rotate, point);
	}

	@Test
	public void testClone() {
		DefaultPoint3D clone = point.clone();
		
		assertEquals(point.getMinX(), clone.getMinX());
		assertEquals(point.getMinY(), clone.getMinY());
		assertEquals(point.getMinZ(), clone.getMinZ());
		assertEquals(point.getMaxX(), clone.getMaxX());
		assertEquals(point.getMaxY(), clone.getMaxY());
		assertEquals(point.getMaxZ(), clone.getMaxZ());
	}
}
