package com.github.skjolber.packing.points3d;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

public class DefaultPoint3DXZYZPlaneTest extends AbstractPointTest {

	private DefaultPoint3D point = new DefaultPoint3D(0, 0, 0, 10, 10, 10, centerPlacement, centerPlacement, null);
	
	// i.e. front and left
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
	// |--------|      
	// |  /     |
	// | /front |
	// |/-------|--------- x
	
	@Test
	public void testSupport() {
		assertTrue(point.isSupportedYZPlane());
		assertFalse(point.isSupportedXYPlane());
		assertTrue(point.isSupportedXZPlane());
		
		assertTrue(point.isSupportedYZPlane(4, 4));
		assertFalse(point.isSupportedXYPlane(4, 4));
		assertTrue(point.isSupportedXZPlane(4, 4));
		
	}
	
	@Test
	public void testSupportCase() {
		
		
	}
	
	// i.e. front and left
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
    // |/ l  *  
	// |    /
	// |--------|      
	// |  /     |
	// | /front |
	// |/-------|--------- x
	@Test
	public void testMoveY() {
		SimplePoint3D move = point.moveY(point.getMinY() + 5);
		
	}

	// i.e. front and left
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
	// |--------|      
	// |  /     |
	// * /front |
	// |/-------|--------- x

	@Test
	public void testMoveZ() {
		SimplePoint3D moveY = point.moveZ(point.getMinZ() + 5);
		
	}
	
	// i.e. front and left
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
	// |--------|      
	// |  /     |
	// | /front |
	// |/---*---|--------- x

	@Test
	public void testMoveX() {
		SimplePoint3D move = point.moveX(point.getMinX() + 5);

		assertFalse(move.isSupportedYZPlane());
		assertTrue(move.isSupportedXZPlane());
	}
	
	//
	// z         y   
	// |
	// |       /|---------|
	// |      / |         |
	// |     /  |         |
	// |    /   |         |
	// |   /    |---------|
	// |  /    /
	// | /    /    
    // |/    /  
	// |    /    
	// |   /     
	// |  / 
	// | /
	// |/---------------- x
	
	@Test
	public void testMoveYSupported() {
		SimplePoint3D move = point.moveY(rearPlacement.getAbsoluteY(), rearPlacement);

		assertTrue(move.isSupportedXZPlane());
		assertFalse(move.isSupportedYZPlane());
	}
	
	//  
	// z         y   
	// |
	// |       /|
	// |      / |
	// |     /  |
	// |    /   |
	// |   /   / 
	// |  /   /  
	// | /   /     
    // |/   /-----/   
	// |   /     /
	// |-----------|      
	// | /     /   |
	// */-----/    |
	// |           |
	// |-----------|--------- x
	
	@Test
	public void testMoveZSupported() {
		SimplePoint3D moveZ = point.moveZ(point.getMinX() + 5, rightPlacement);

		assertTrue(moveZ.isSupportedXYPlane());
		assertTrue(moveZ.isSupportedYZPlane());
		assertTrue(moveZ.isSupportedXZPlane());
	}

	//  
	// z         y   
	// |
	// |         
	// |         
	// |         
	// |         /|
	// |        / |
	// |       /  |
	// |      /   |
    // |     /    /
	// |    /    /
	// |    |------|      
	// |    |  /   |
	// |    | /    |
	// |----|*-----|--------- x
	
	@Test
	public void testMoveXSupported() {
		SimplePoint3D moveX = point.moveX(point.getMinX() + 5, rightPlacement);

		assertTrue(moveX.isSupportedYZPlane());
		assertTrue(moveX.isSupportedXZPlane());
	}

	@Test
	public void testClone() {
		DefaultPoint3D clone = point.clone();
		
		assertEquals(point.getMinX(), clone.getMinX());
		assertEquals(point.getMinY(), clone.getMinY());
		assertEquals(point.getMinZ(), clone.getMinZ());
		assertEquals(point.getMaxX(), clone.getMaxX());
		assertEquals(point.getMaxY(), clone.getMaxY());
		assertEquals(point.getMaxZ(), clone.getMaxZ());
		
		assertSame(point.getYZPlane(), clone.getYZPlane());
	}
}
