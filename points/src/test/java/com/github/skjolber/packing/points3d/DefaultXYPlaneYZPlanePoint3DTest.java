package com.github.skjolber.packing.points3d;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.ep.points3d.DefaultXYPlaneYZPlanePoint3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

public class DefaultXYPlaneYZPlanePoint3DTest extends AbstractPointTest {

	private DefaultXYPlaneYZPlanePoint3D point = new DefaultXYPlaneYZPlanePoint3D(0, 0, 0, 10, 10, 10, centerPlacement, centerPlacement);
	
	// i.e. buttom and left
	//  
	// z         y   
	// |
	// |       /|
	// |      / |
	// |     /  |
	// |    /   |
	// |   / t  |
	// |  / f  /-------/
	// | / e  /  m    / 
    // |/ l  /  o    /
	// |    /  t    /
	// |   /  t    /
	// |  /  o    /
	// | /  b    /
	// |/-------/--------- x
	
	@Test
	public void testSupport() {
		assertTrue(point.isSupportedXYPlane());
		assertTrue(point.isSupportedYZPlane());
		assertFalse(point.isSupportedXZPlane());
		
		assertTrue(point.isSupportedXYPlane(4, 4));
		assertFalse(point.isSupportedXZPlane(4, 4));
		assertTrue(point.isSupportedYZPlane(4, 4));

		assertEquals(point.calculateXZSupport(10, 10), 0);
	}
	
	@Test
	public void testSupportCase() {
		assertEquals(point.calculateXYSupport(5, 5), 25);
		assertEquals(point.calculateXYSupport(10, 10), 100);
		assertEquals(point.calculateXYSupport(11, 11), 100);
		
		assertEquals(point.calculateYZSupport(5, 5), 25);
		assertEquals(point.calculateYZSupport(10, 10), 100);
		assertEquals(point.calculateYZSupport(11, 11), 100);
	}
	
	// i.e. buttom and left
	//  
	// z         y   
	// |
	// |       /|
	// |      / |
	// |     /  |
	// |    /   |
	// |   / t  |
	// |  / f  /-------/
	// | / e  /  m    / 
    // |/ l  /  o    /
	// |    /  t    /
	// |   /  t    /
	// |  /  o    /
	// | /  b    /
	// |/---*---/--------- x

	@Test
	public void testMoveX() {
		SimplePoint3D moveX = point.moveX(point.getMinX() + 5);
		
		assertEquals(moveX.calculateXYSupport(5, 5), 25);
		assertEquals(moveX.calculateXYSupport(5, 10), 50);
		assertEquals(moveX.calculateXYSupport(5, 11), 50);
		
		assertFalse(moveX.isSupportedYZPlane());
	}

	// i.e. buttom and left
	//  
	// z         y   
	// |
	// |       /|
	// |      / |
	// |     /  |
	// |    /   |
	// |   / t  |
	// |  / f  /-------/
	// | / e  /  m    / 
    // |/ l  /  o    /
	// |    *  t    /
	// |   /  t    /
	// |  /  o    /
	// | /  b    /
	// |/-------/--------- x

	@Test
	public void testMoveY() {
		SimplePoint3D moveY = point.moveY(point.getMinY() + 5);
		
		assertEquals(moveY.calculateXYSupport(5, 5), 25);
		assertEquals(moveY.calculateXYSupport(10, 5), 50);
		assertEquals(moveY.calculateXYSupport(11, 5), 50);
		
		assertEquals(moveY.calculateYZSupport(5, 5), 25);
		assertEquals(moveY.calculateYZSupport(5, 10), 50);
		assertEquals(moveY.calculateYZSupport(5, 11), 50);
	}
	
	// i.e. buttom and left
	//  
	// z         y   
	// |
	// |       /|
	// |      / |
	// |     /  |
	// |    /   |
	// |   / t  |
	// |  / f  /-------/
	// | / e  /  m    / 
    // |/ l  /  o    /
	// |    /  t    /
	// *   /  t    /
	// |  /  o    /
	// | /  b    /
	// |/-------/--------- x


	@Test
	public void testMoveZ() {
		SimplePoint3D moveZ = point.moveZ(point.getMinZ() + 5);

		assertFalse(moveZ.isSupportedXYPlane());
		
		assertTrue(moveZ.isSupportedYZPlane());
		assertFalse(moveZ.isSupportedXZPlane());

	}

	// i.e. buttom and left
	//  
	// z         y   
	// |
	// |       /|
	// |      / |
	// |     /  |
	// |    /   |
	// |   / t  |
	// |  / f  /-------/
	// | / e  /  m    / 
    // |/ l  /  o    /
	// |    /  t    /
	// |   /  t    /
	// |  /  o    /
	// | /  b    /
	// |/-------/--------- x

	
	@Test
	public void testMoveXSupported() {
		SimplePoint3D moveZ = point.moveX(topPlacement.getAbsoluteX(), topPlacement);

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
		SimplePoint3D moveZ = point.moveY(topPlacement.getAbsoluteY(), topPlacement);

		assertTrue(moveZ.isSupportedXYPlane());
		assertTrue(moveZ.isSupportedXZPlane());
	}

	@Test
	public void testMoveZSupported() {
		SimplePoint3D moveZ = point.moveZ(topPlacement.getAbsoluteZ(), topPlacement);

		assertTrue(moveZ.isSupportedXYPlane());
		assertFalse(moveZ.isSupportedYZPlane());
	}
	
	@Test
	public void testClone() {
		DefaultXYPlaneYZPlanePoint3D clone = point.clone();
		
		assertEquals(point.getMinX(), clone.getMinX());
		assertEquals(point.getMinY(), clone.getMinY());
		assertEquals(point.getMinZ(), clone.getMinZ());
		assertEquals(point.getMaxX(), clone.getMaxX());
		assertEquals(point.getMaxY(), clone.getMaxY());
		assertEquals(point.getMaxZ(), clone.getMaxZ());
		
		assertSame(point.getXYPlane(), clone.getXYPlane());
		assertSame(point.getYZPlane(), clone.getYZPlane());
	}
}
