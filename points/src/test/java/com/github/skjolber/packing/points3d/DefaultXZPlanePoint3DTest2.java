package com.github.skjolber.packing.points3d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.DefaultXZPlanePoint3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

public class DefaultXZPlanePoint3DTest2 extends AbstractPointTest {

	private Placement placement1 = createStackPlacement(0, 0, 0, 9, 9, 9);
	private Placement placement2 = createStackPlacement(0, 10, 0, 4, 14, 4);
	
	private DefaultXZPlanePoint3D point = new DefaultXZPlanePoint3D(0, 10, 0, 10, 10, 10, placement1);
	
	@Test
	public void testSupport() {
		assertTrue(point.isSupportedXZPlane());
		assertFalse(point.isSupportedXYPlane());
		assertFalse(point.isSupportedYZPlane());
		
		assertEquals(point.calculateXYSupport(10, 10), 0);
		assertEquals(point.calculateYZSupport(10, 10), 0);
	}
	
	@Test
	public void testSupportCase() {
		assertEquals(point.calculateXZSupport(5, 5), 25);
		assertEquals(point.calculateXZSupport(10, 10), 100);
		assertEquals(point.calculateXZSupport(11, 11), 100);
	}
	
	//   x
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
	public void testMoveX() {
		SimplePoint3D moveX = point.moveX(point.getMinX() + 5);
		
		assertEquals(moveX.calculateXZSupport(5, 5), 25);
		assertEquals(moveX.calculateXZSupport(5, 10), 50);
		assertEquals(moveX.calculateXZSupport(5, 11), 50);
	}

	//    x
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
		
		assertEquals(moveY.calculateXZSupport(5, 5), 25);
		assertEquals(moveY.calculateXZSupport(10, 5), 50);
		assertEquals(moveY.calculateXZSupport(11, 5), 50);
	}

	@Test
	public void testMoveY() {
		SimplePoint3D moveY = point.moveY(point.getMinY() + 5);

		assertFalse(moveY.isSupportedXZPlane());
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
	// |----------*------------ x
	//

	
	@Test
	public void testMoveXSupported() {
		SimplePoint3D moveX = point.moveX(placement2.getAbsoluteX(), placement2);

		assertTrue(moveX.isSupportedXZPlane());
		assertTrue(moveX.isSupportedYZPlane());
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
	// |----------------------- x
	//
	
	@Test
	public void testMoveZSupported() {
		SimplePoint3D moveZ = point.moveZ(placement2.getAbsoluteZ(), placement2);

		assertTrue(moveZ.isSupportedXYPlane());
		assertTrue(moveZ.isSupportedXZPlane());
	}

}
