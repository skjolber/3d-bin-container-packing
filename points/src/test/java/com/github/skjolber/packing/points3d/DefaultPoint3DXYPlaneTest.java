package com.github.skjolber.packing.points3d;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

public class DefaultPoint3DXYPlaneTest extends AbstractPointTest {

	private DefaultPoint3D point = new DefaultPoint3D(0, 0, 10, 10, 10, 10, null, null, centerPlacement);
	
	// i.e. buttom
	//  
	// z         y   
	// |            
	// |     /--------/
	// |    /        /
	// |   /        /      
	// |  /        /
	// | /        /
	// |/--------/--------- x
	
	@Test
	public void testSupport() {
		assertTrue(point.isSupportedXYPlane());
		assertFalse(point.isSupportedYZPlane());
		assertFalse(point.isSupportedXZPlane());
		
		assertTrue(point.isSupportedXYPlane(4, 4));
		assertFalse(point.isSupportedXZPlane(4, 4));
		assertFalse(point.isSupportedYZPlane(4, 4));

	}
	
	@Test
	public void testSupportCase() {
	}
	
	//  
	// z         y   
	// |            
	// |         /----/
	// |        /    /
	// |       /    /      
	// |      /    /
	// |     /    /
	// |-----*---/--------- x

	@Test
	public void testMoveX() {
		SimplePoint3D moveX = point.moveX(point.getMinX() + 5);
		
	}

	//  
	// z         y   
	// |            
	// |     /--------/
	// |    /        /
	// |   *--------/      
	// |  /        
	// | /        
	// |/------------------ x

	@Test
	public void testMoveY() {
		SimplePoint3D moveY = point.moveY(point.getMinY() + 5);
		
	}

	@Test
	public void testMoveZ() {
		SimplePoint3D moveZ = point.moveZ(point.getMinZ() + 5);

		assertFalse(moveZ.isSupportedXYPlane());
	}
	
	//               
	// z          /|  
	// |         / | 
	// |        /  /---/
	// |       /  /   /
	// |      /  /   /      
	// |      | /   /
	// |      |/   /
	// |------*---/--------- x
	
	@Test
	public void testMoveXSupported() {
		SimplePoint3D move = point.moveX(innerPlacement.getAbsoluteX(), innerPlacement);

		assertTrue(move.isSupportedXYPlane());
		assertTrue(move.isSupportedYZPlane());
	}
	
	//  
	// z         y   
	// |            
	// |        /--------/
	// |       /        /
	// |      /        /
	// |  |--------|  /
	// |  |        | /
	// |  |        |/      
	// |  /--------/
	// | /         
	// |/----------------- x
	
	@Test
	public void testMoveYSupported() {
		SimplePoint3D moveZ = point.moveY(topPlacement.getAbsoluteY(), topPlacement);

		assertTrue(moveZ.isSupportedXYPlane());
		assertTrue(moveZ.isSupportedXZPlane());
	}

	//  
	// z         y   
	// |            
	// |            
	// |    /-------/        
	// |   /       /
	// |  /       /
	// | /       /
	// |/       /      
	// *-------- 
	// |
	// |----------------- x
	
	
	@Test
	public void testMoveZSupported() {
		SimplePoint3D moveZ = point.moveZ(topPlacement.getAbsoluteZ(), topPlacement);

		assertTrue(moveZ.isSupportedXYPlane());
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
	}
}
