package com.github.skjolber.packing.points3d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.DefaultYZPlanePoint3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

public class DefaultYZPlanePoint3DTest3 extends AbstractPointTest {

	private Placement placement1 = createStackPlacement(0, 0, 0, 9, 9, 9);
	private Placement placement2 = createStackPlacement(10, 0, 0, 14, 4, 4);
	
	private DefaultYZPlanePoint3D point = new DefaultYZPlanePoint3D(10, 0, 0, 10, 10, 10, placement1);
	
	@Test
	public void testSupport() {
		assertTrue(point.isSupportedYZPlane());
		assertFalse(point.isSupportedXYPlane());
		assertFalse(point.isSupportedXZPlane());
		
		assertEquals(point.calculateXYSupport(10, 10), 0);
		assertEquals(point.calculateXZSupport(10, 10), 0);
	}
	
	@Test
	public void testSupportCase() {
		assertEquals(point.calculateYZSupport(5, 5), 25);
		assertEquals(point.calculateYZSupport(10, 10), 100);
		assertEquals(point.calculateYZSupport(11, 11), 100);
	}
	
	//   y
	//   |
	//   | 
	//   |-------------------|
	//   |                   | 
	//   |                   | 
	// 5 * - - - - - - - - - | 
	//   |                   | 
	//   |                   | 
	//   |----------------------- z
	//                       10
	
	@Test
	public void testMoveY() {
		SimplePoint3D move = point.moveY(point.getMinY() + 5);
		
		assertEquals(move.calculateYZSupport(5, 5), 25);
		assertEquals(move.calculateYZSupport(5, 10), 50);
		assertEquals(move.calculateYZSupport(5, 11), 50);
	}

	//    y
	//    |
	//    | 
	// 10 |----------|--------|
	//    |                   | 
	//    |          |        | 
	//    |                   | 
	//    |          |        | 
	//    |                   | 
	//    |----------*------------ z
	//               5

	@Test
	public void testMoveZ() {
		SimplePoint3D moveY = point.moveZ(point.getMinZ() + 5);
		
		assertEquals(moveY.calculateYZSupport(5, 5), 25);
		assertEquals(moveY.calculateYZSupport(10, 5), 50);
		assertEquals(moveY.calculateYZSupport(11, 5), 50);
	}

	@Test
	public void testMoveX() {
		SimplePoint3D move = point.moveX(point.getMinX() + 5);

		assertFalse(move.isSupportedYZPlane());
	}
	
	// z
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
		SimplePoint3D move = point.moveY(placement2.getAbsoluteY(), placement2);

		assertTrue(move.isSupportedXZPlane());
		assertTrue(move.isSupportedYZPlane());
	}
	
	// z
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
	public void testMoveZSupported() {
		SimplePoint3D moveZ = point.moveZ(placement2.getAbsoluteZ(), placement2);

		assertTrue(moveZ.isSupportedXYPlane());
		assertTrue(moveZ.isSupportedYZPlane());
	}

}
