package com.github.skjolber.packing.points3d;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

public class DefaultPoint3DYZPlaneTest extends AbstractPointTest {

	private DefaultPoint3D point = new DefaultPoint3D(10, 0, 0, 10, 10, 10, centerPlacement, null, null);
	
	// i.e. left
	//  
	// z         y   
	// |
	// |       /|
	// |      / |
	// |     /  |
	// |    /   |
	// |   / t  |
	// |  / f  /
	// | / e  /    
    // |/ l  /  
	// |    /
	// |   /      
	// |  /
	// | /
	// |/--------------- x

	@Test
	public void testSupport() {
		assertTrue(point.isSupportedYZPlane());
		assertFalse(point.isSupportedXYPlane());
		assertFalse(point.isSupportedXZPlane());
		
		assertTrue(point.isSupportedYZPlane(4, 4));
		assertFalse(point.isSupportedXYPlane(4, 4));
		assertFalse(point.isSupportedXZPlane(4, 4));
		
	}
	
	@Test
	public void testSupportCase() {
	}
	
	//  
	// z         y   
	// |
	// |       /|
	// |      / |
	// |     /  |
	// |    /   |
	// |   /    |
	// |   |   /
	// |   |  /    
    // |   | /  
	// |   |/
	// |   *      
	// |  /
	// | /
	// |/--------------- x
	
	@Test
	public void testMoveY() {
		SimplePoint3D move = point.moveY(point.getMinY() + 5);
		
	}

	//  
	// z         y   
	// |
	// |       /|
	// |      / |
	// |     /  |
	// |    /  /
	// |   /  /
	// |  /  /
	// | /  /    
    // |/  /  
	// |  /
	// | /      
	// */
	// |
	// |
	// |--------------- x
	
	@Test
	public void testMoveZ() {
		SimplePoint3D moveY = point.moveZ(point.getMinZ() + 5);
		
	}

	@Test
	public void testMoveX() {
		SimplePoint3D move = point.moveX(point.getMinX() + 5);

		assertFalse(move.isSupportedYZPlane());
	}

	//         /|   y
	//        / |  
	// z     /  |    
	// |    /   /   
	// |   |------|
	// |   |  /   |
    // |   | /    |
	// |   |/     |
	// |   *-------
	// |  /
	// | /
	// |/--------------- x
	
	@Test
	public void testMoveYSupported() {
		SimplePoint3D move = point.moveY(rightPlacement.getAbsoluteY(), rightPlacement);

		assertTrue(move.isSupportedXZPlane());
		assertTrue(move.isSupportedYZPlane());
	}
	
	//  
	// z         y   
	// |
	// |       /|
	// |      / |
	// |     /  |----
	// |    /  /   /
	// |   /  /   /
	// |  /  /   /
	// | /  /   / 
    // |/  /   / 
	// |  /   /
	// | /   /    
	// */---/
	// |
	// |
	// |--------------- x
	
	@Test
	public void testMoveZSupported() {
		SimplePoint3D moveZ = point.moveZ(rightPlacement.getAbsoluteZ(), rightPlacement);

		assertTrue(moveZ.isSupportedXYPlane());
		assertTrue(moveZ.isSupportedYZPlane());
	}

	// i.e. left
	//  
	// z         y   
	// |            
	// |           /|
	// |          / |
	// |         /  |    
    // |        /   |  
	// |       /    |
	// |      /    /      
	// |     |    / 
	// |     |   /
	// |     |  /
	// |     | /
	// |-----*---------- x
	
	@Test
	public void testMoveXSupported() {
		SimplePoint3D moveX = point.moveX(rightPlacement.getAbsoluteX(), rightPlacement);

		assertTrue(moveX.isSupportedYZPlane());
		assertFalse(moveX.isSupportedXZPlane());
		assertFalse(moveX.isSupportedXYPlane());
	}

	@Test
	public void testCopy() {
		DefaultPoint3D clone = point.copy();
		
		assertEquals(point.getMinX(), clone.getMinX());
		assertEquals(point.getMinY(), clone.getMinY());
		assertEquals(point.getMinZ(), clone.getMinZ());
		assertEquals(point.getMaxX(), clone.getMaxX());
		assertEquals(point.getMaxY(), clone.getMaxY());
		assertEquals(point.getMaxZ(), clone.getMaxZ());
		
		assertSame(point.getYZPlane(), clone.getYZPlane());
	}
}
