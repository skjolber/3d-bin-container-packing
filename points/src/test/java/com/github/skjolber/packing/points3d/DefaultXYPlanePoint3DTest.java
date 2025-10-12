package com.github.skjolber.packing.points3d;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.DefaultXYPlanePoint3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

public class DefaultXYPlanePoint3DTest extends AbstractPointTest {

	private DefaultXYPlanePoint3D point = new DefaultXYPlanePoint3D(0, 0, 10, 10, 10, 10, centerPlacement);
	
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

		assertEquals(point.calculateXZSupport(10, 10), 0);
		assertEquals(point.calculateYZSupport(10, 10), 0);
	}
	
	@Test
	public void testSupportCase() {
		assertEquals(point.calculateXYSupport(5, 5), 25);
		assertEquals(point.calculateXYSupport(10, 10), 100);
		assertEquals(point.calculateXYSupport(11, 11), 100);
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
		
		assertEquals(moveX.calculateXYSupport(5, 5), 25);
		assertEquals(moveX.calculateXYSupport(5, 10), 50);
		assertEquals(moveX.calculateXYSupport(5, 11), 50);
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
		
		assertEquals(moveY.calculateXYSupport(5, 5), 25);
		assertEquals(moveY.calculateXYSupport(10, 5), 50);
		assertEquals(moveY.calculateXYSupport(11, 5), 50);
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
	public void testClone() {
		DefaultXYPlanePoint3D clone = point.clone();
		
		assertEquals(point.getMinX(), clone.getMinX());
		assertEquals(point.getMinY(), clone.getMinY());
		assertEquals(point.getMinZ(), clone.getMinZ());
		assertEquals(point.getMaxX(), clone.getMaxX());
		assertEquals(point.getMaxY(), clone.getMaxY());
		assertEquals(point.getMaxZ(), clone.getMaxZ());
		
		assertSame(point.getXYPlane(), clone.getXYPlane());
	}
}
