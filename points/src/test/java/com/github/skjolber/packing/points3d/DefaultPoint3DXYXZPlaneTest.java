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

public class DefaultPoint3DXYXZPlaneTest extends AbstractPointTest {

	private DefaultPoint3D point = new DefaultPoint3D(0, 0, 0, 10, 10, 10, null, centerPlacement, centerPlacement);
	
	// i.e. buttom and front
	//  
	// z         y   
	// |            
	// |       /--------/
	// |      /        /  
    // |     / bottom / 
	// |    /        /
	// |--------|   /      
	// |  /     |  /
	// | /front | /
	// |/-------|/--------- x
	
	@Test
	public void testSupport() {
		assertTrue(point.isSupportedXYPlane());
		assertFalse(point.isSupportedYZPlane());
		assertTrue(point.isSupportedXZPlane());
		
		assertTrue(point.isSupportedXYPlane(4, 4));
		assertFalse(point.isSupportedYZPlane(4, 4));
		assertTrue(point.isSupportedXZPlane(4, 4));

	}
	
	@Test
	public void testSupportCase() {
		
	}
	
	// i.e. buttom and front
	//  
	// z         y   
	// |            
	// |       /--------/
	// |      /        /  
    // |     / bottom / 
	// |    /        /
	// |--------|   /      
	// |  /     |  /
	// | /front | /
	// |/---*---|/--------- x

	@Test
	public void testMoveX() {
		SimplePoint3D moveX = point.moveX(point.getMinX() + 5);
		
		
	}

	// i.e. buttom and front
	//  
	// z         y   
	// |            
	// |       /--------/
	// |      /        /  
    // |     / bottom / 
	// |    *        /
	// |--------|   /      
	// |  /     |  /
	// | /front | /
	// |/-------|/--------- x

	@Test
	public void testMoveY() {
		SimplePoint3D moveY = point.moveY(point.getMinY() + 5);
		
		
		assertFalse(moveY.isSupportedXZPlane());
	}
		
	//  
	// z         y   
	// |            
	// |        /---------/
	// |       /         /  
    // |      /         / 
	// |     /         /
	// |---------|    /      
	// |   /     |   /
	// *  /      |  /
	// | /       | /
	// |/--------|/--------- x

	@Test
	public void testMoveZ() {
		SimplePoint3D moveZ = point.moveZ(point.getMinZ() + 5);

		assertTrue(moveZ.isSupportedXZPlane());
	}
	
	//  
	// z         y   /|
	// |            / |
	// |        /--/--/----/
	// |       /  /  /    /  
    // |      /  /  /    / 
	// |     /  /  /    /
	// |----------|    /      
	// |   /  /  /|   /
	// |  /   | / |  /
	// | /    |/  | /
	// |/-----*---|/--------- x
	
	
	@Test
	public void testMoveXSupported() {
		SimplePoint3D moveZ = point.moveX(topPlacement.getAbsoluteX(), topPlacement);

		assertTrue(moveZ.isSupportedXYPlane());
		assertTrue(moveZ.isSupportedYZPlane());
		assertTrue(moveZ.isSupportedXZPlane());
	}

	@Test
	public void testMoveYSupported() {
		SimplePoint3D moveZ = point.moveY(topPlacement.getAbsoluteY(), topPlacement);

		assertTrue(moveZ.isSupportedXYPlane());
		assertTrue(moveZ.isSupportedXZPlane());
	}

	// x
	// |
	// |---| 
	// |   |
	// *------------ y
	//

	@Test
	public void testMoveZSupported() {
		SimplePoint3D moveZ = point.moveZ(topPlacement.getAbsoluteZ(), topPlacement);

		assertTrue(moveZ.isSupportedXYPlane());
		assertFalse(moveZ.isSupportedXZPlane());
		assertFalse(moveZ.isSupportedYZPlane());
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
		
		assertSame(point.getXYPlane(), copy.getXYPlane());
		assertSame(point.getXZPlane(), copy.getXZPlane());
	}
}
