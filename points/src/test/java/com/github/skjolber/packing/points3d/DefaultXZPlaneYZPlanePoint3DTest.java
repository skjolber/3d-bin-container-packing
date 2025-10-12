package com.github.skjolber.packing.points3d;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.DefaultXZPlanePoint3D;
import com.github.skjolber.packing.ep.points3d.DefaultXZPlaneYZPlanePoint3D;
import com.github.skjolber.packing.ep.points3d.DefaultYZPlanePoint3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

public class DefaultXZPlaneYZPlanePoint3DTest extends AbstractPointTest {

	private DefaultXZPlaneYZPlanePoint3D point = new DefaultXZPlaneYZPlanePoint3D(0, 0, 0, 10, 10, 10, centerPlacement, centerPlacement);
	
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
		
		assertEquals(point.calculateXYSupport(10, 10), 0);
	}
	
	@Test
	public void testSupportCase() {
		assertEquals(point.calculateYZSupport(5, 5), 25);
		assertEquals(point.calculateYZSupport(10, 10), 100);
		assertEquals(point.calculateYZSupport(11, 11), 100);
		
		assertEquals(point.calculateXZSupport(5, 5), 25);
		assertEquals(point.calculateXZSupport(10, 10), 100);
		assertEquals(point.calculateXZSupport(11, 11), 100);
		
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
		
		assertEquals(move.calculateYZSupport(5, 5), 25);
		assertEquals(move.calculateYZSupport(5, 10), 50);
		assertEquals(move.calculateYZSupport(5, 11), 50);
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
		
		assertEquals(moveY.calculateYZSupport(5, 5), 25);
		assertEquals(moveY.calculateYZSupport(10, 5), 50);
		assertEquals(moveY.calculateYZSupport(11, 5), 50);
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
		DefaultXZPlaneYZPlanePoint3D clone = point.clone();
		
		assertEquals(point.getMinX(), clone.getMinX());
		assertEquals(point.getMinY(), clone.getMinY());
		assertEquals(point.getMinZ(), clone.getMinZ());
		assertEquals(point.getMaxX(), clone.getMaxX());
		assertEquals(point.getMaxY(), clone.getMaxY());
		assertEquals(point.getMaxZ(), clone.getMaxZ());
		
		assertSame(point.getYZPlane(), clone.getYZPlane());
	}
}
