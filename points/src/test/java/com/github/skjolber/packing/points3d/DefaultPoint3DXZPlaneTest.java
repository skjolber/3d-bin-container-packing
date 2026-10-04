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

public class DefaultPoint3DXZPlaneTest extends AbstractPointTest {

	private DefaultPoint3D point = new DefaultPoint3D(0, 10, 0, 10, 10, 10, null, centerPlacement, null);
	
	// i.e. front
	//  
	// z           
	// |
	// |        y
	// |       /    
    // |      /  
	// |     /
	// |----------|      
	// |          |
	// |          |
	// |          |
	// |----------|----- x
	
	@Test
	public void testSupport() {
		assertTrue(point.isSupportedXZPlane());
		assertFalse(point.isSupportedXYPlane());
		assertFalse(point.isSupportedYZPlane());
		
		assertTrue(point.isSupportedXZPlane(4, 4));
		assertFalse(point.isSupportedXYPlane(4, 4));
		assertFalse(point.isSupportedYZPlane(4, 4));
		
	}
	
	@Test
	public void testSupportCase() {
	}
	
	// i.e. front
	//  
	// z           
	// |
	// |        y
	// |       /    
    // |      /  
	// |     /
	// |    /|-----|      
	// |   / |     |
	// |  /  |     |
	// | /   |     |
	// |-----*-----|----- x
	
	@Test
	public void testMoveX() {
		SimplePoint3D moveX = point.moveX(point.getMinX() + 5);
		
	}

	//  
	// z           
	// |
	// |         y
	// |        /    
    // |       /  
	// |      /
	// |----------|      
	// |          |
	// *----------|
	// |  /        
	// | /         
	// |--------------- x

	@Test
	public void testMoveZ() {
		SimplePoint3D moveY = point.moveZ(point.getMinZ() + 5);
		
	}
	
	@Test
	public void testMoveY() {
		SimplePoint3D moveY = point.moveY(point.getMinY() + 5);

		assertFalse(moveY.isSupportedXZPlane());
	}
	//            y  
	//             /|  
	// z          / |  
	// |         /  | 
	// |        /   /
	// |       /   / 
	// |      /------|      
	// |      |  /   |
	// |      | /    |
	// |------*/----------- x

	@Test
	public void testMoveXSupported() {
		SimplePoint3D moveX = point.moveX(rearPlacement.getAbsoluteX(), rearPlacement);

		assertTrue(moveX.isSupportedXZPlane());
		assertTrue(moveX.isSupportedYZPlane());
	}
	
	//  
	// z           
	// |             y
	// |           
	// |      /-----------/
    // |     /           /
	// |    /           /
	// |   /           /  
	// |----------|   /
	// | /        |  /
	// |/         | /
	// *----------|/
	// |           
	// |           
	// |--------------- x
	
	@Test
	public void testMoveZSupported() {
		SimplePoint3D moveZ = point.moveZ(rearPlacement.getAbsoluteZ(), rearPlacement);

		assertTrue(moveZ.isSupportedXYPlane());
		assertTrue(moveZ.isSupportedXZPlane());
	}
	
	//  
	// z           
	// |           y  
	// |          /
	// |    ----------
	// |    |        |
    // |    |        |   
	// |    |        |  
	// |    |        |     
	// |    *---------    
	// |   /         
	// |  /       
	// | /        
	// |/-------------- x

	@Test
	public void testMoveYSupported() {
		SimplePoint3D moveY = point.moveY(rearPlacement.getAbsoluteY(), rearPlacement);

		assertTrue(moveY.isSupportedXZPlane());
		assertFalse(moveY.isSupportedYZPlane());
		assertFalse(moveY.isSupportedXYPlane());
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
		
		assertSame(point.getXZPlane(), clone.getXZPlane());
	}
}
