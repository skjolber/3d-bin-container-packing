package com.github.skjolber.packing.points3d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.Default3DPlanePoint3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

public class Default3DPlanePoint3DTest extends AbstractPointTest {

	private Placement placement1 = createStackPlacement(0, 0, 0, 100, 100, 100);
	private Placement placement2 = createStackPlacement(0, 0, 0, 4, 4, 4);
	
	private Default3DPlanePoint3D point = new Default3DPlanePoint3D(0, 0, 0, 10, 10, 10, placement1, placement1, placement1);
	
	@Test
	public void testSupport() {
		assertTrue(point.isSupportedXZPlane());
		assertTrue(point.isSupportedXYPlane());
		assertTrue(point.isSupportedYZPlane());
		
		assertEquals(point.calculateXYSupport(10, 10), 100);
		assertEquals(point.calculateYZSupport(10, 10), 100);
		assertEquals(point.calculateXZSupport(10, 10), 100);
	}
	
	@Test
	public void testMoveX() {
		SimplePoint3D move = point.moveX(5);
		
		assertTrue(move.isSupportedXYPlane());
		assertTrue(move.isSupportedXZPlane());
		assertFalse(move.isSupportedYZPlane());
		
		assertEquals(move.calculateXYSupport(10, 10), 100);
		assertEquals(move.calculateXZSupport(10, 10), 100);
	}

	@Test
	public void testMoveY() {
		SimplePoint3D move = point.moveY(5);
		
		assertFalse(move.isSupportedXZPlane());
		assertTrue(move.isSupportedXYPlane());
		assertTrue(move.isSupportedYZPlane());
		
		assertEquals(move.calculateXYSupport(10, 10), 100);
		assertEquals(move.calculateYZSupport(10, 10), 100);
	}

	@Test
	public void testMoveZ() {
		SimplePoint3D move = point.moveZ(5);
		
		assertTrue(move.isSupportedXZPlane());
		assertFalse(move.isSupportedXYPlane());
		assertTrue(move.isSupportedYZPlane());
		
		assertEquals(move.calculateYZSupport(10, 10), 100);
		assertEquals(move.calculateXZSupport(10, 10), 100);
	}

	@Test
	public void testMoveXPlacement() {
		SimplePoint3D move = point.moveX(5, placement2);
		
		assertTrue(move.isSupportedXYPlane());
		assertTrue(move.isSupportedXZPlane());
		assertTrue(move.isSupportedYZPlane());
		
		assertEquals(move.calculateXYSupport(10, 10), 100);
		assertEquals(move.calculateXZSupport(10, 10), 100);
		assertEquals(move.calculateYZSupport(10, 10), 25);
	}

	@Test
	public void testMoveYPlacement() {
		SimplePoint3D move = point.moveY(5, placement2);
		
		assertTrue(move.isSupportedXZPlane());
		assertTrue(move.isSupportedXYPlane());
		assertTrue(move.isSupportedYZPlane());
		
		assertEquals(move.calculateXYSupport(10, 10), 100);
		assertEquals(move.calculateYZSupport(10, 10), 100);
		assertEquals(move.calculateXZSupport(10, 10), 25);
	}

	@Test
	public void testMoveZPlacement() {
		SimplePoint3D move = point.moveZ(5, placement2);
		
		assertTrue(move.isSupportedXZPlane());
		assertTrue(move.isSupportedXYPlane());
		assertTrue(move.isSupportedYZPlane());
		
		assertEquals(move.calculateYZSupport(10, 10), 100);
		assertEquals(move.calculateXZSupport(10, 10), 100);
		assertEquals(move.calculateXYSupport(5, 5), 25);
	}
	
}
