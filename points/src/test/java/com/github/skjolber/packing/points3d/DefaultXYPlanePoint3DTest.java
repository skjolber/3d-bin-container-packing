package com.github.skjolber.packing.points3d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.DefaultXYPlanePoint3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

public class DefaultXYPlanePoint3DTest extends AbstractPointTest {

	private Placement placement1 = createStackPlacement(0, 0, 0, 9, 9, 9);
	private Placement placement2 = createStackPlacement(0, 0, 10, 4, 4, 14);
	
	private DefaultXYPlanePoint3D point = new DefaultXYPlanePoint3D(0, 0, 10, 10, 10, 10, placement1);
	
	@Test
	public void testSupport() {
		assertTrue(point.isSupportedXYPlane());
		assertFalse(point.isSupportedYZPlane());
		assertFalse(point.isSupportedXZPlane());
		
		assertEquals(point.calculateXZSupport(10, 10), 0);
		assertEquals(point.calculateYZSupport(10, 10), 0);
	}
	
	@Test
	public void testSupportCase() {
		assertEquals(point.calculateXYSupport(5, 5), 25);
		assertEquals(point.calculateXYSupport(10, 10), 100);
		assertEquals(point.calculateXYSupport(11, 11), 100);
	}
	
	// x
	// |
	// | 
	// |-------------------|
	// |                   | 
	// |                   | 
	// * - - - - - - - - - | 
	// |                   | 
	// |                   | 
	// |----------------------- y

	@Test
	public void testMoveX() {
		SimplePoint3D moveX = point.moveX(point.getMinX() + 5);
		
		assertEquals(moveX.calculateXYSupport(5, 5), 25);
		assertEquals(moveX.calculateXYSupport(5, 10), 50);
		assertEquals(moveX.calculateXYSupport(5, 11), 50);
	}

	// x
	// |
	// | 
	// |----------|--------|
	// |                   | 
	// |          |        | 
	// |                   | 
	// |          |        | 
	// |                   | 
	// |----------*------------ y

	@Test
	public void testMoveY() {
		SimplePoint3D moveY = point.moveY(point.getMinY() + 5);
		
		assertEquals(moveY.calculateXYSupport(5, 5), 25);
		assertEquals(moveY.calculateXYSupport(10, 5), 50);
		assertEquals(moveY.calculateXYSupport(11, 5), 50);
	}

	@Test
	public void testMoveZ() {
		SimplePoint3D moveZ = point.moveZ(point.getMinZ() + 5);

		assertFalse(moveZ.isSupportedXYPlane());
	}
	
	// x
	// |
	// | 
	// |-------------------|
	// |                   | 
	// |                   | 
	// *==========|        | 
	// |          |        | 
	// |          |        | 
	// |----------------------- y
	//
	
	@Test
	public void testMoveXSupported() {
		SimplePoint3D moveZ = point.moveX(placement2.getAbsoluteX(), placement2);

		assertTrue(moveZ.isSupportedXYPlane());
		assertTrue(moveZ.isSupportedYZPlane());
	}
	
	// x
	// |
	// | 
	// |-------------------|
	// |                   | 
	// |                   | 
	// |----------║        | 
	// |          ║        | 
	// |          ║        | 
	// |----------*------------ y
	//
	
	@Test
	public void testMoveYSupported() {
		SimplePoint3D moveZ = point.moveY(placement2.getAbsoluteY(), placement2);

		assertTrue(moveZ.isSupportedXYPlane());
		assertTrue(moveZ.isSupportedXZPlane());
	}

}
