package com.github.skjolber.packing.ep.points3d;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;

public abstract class SimplePoint3D extends Point {
	
	public SimplePoint3D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public boolean isSupportedXYPlane(int x, int y) { // i.e. z is fixed
		return false;
	}

	public boolean isSupportedYZPlane(int y, int z) { // i.e. x is fixed
		return false;
	}

	public boolean isSupportedXZPlane(int x, int z) { // i.e. y is fixed
		return false;
	}
	
	public boolean isSupportedXYPlane() { // i.e. z is fixed
		return false;
	}

	public boolean isSupportedYZPlane() { // i.e. x is fixed
		return false;
	}

	public boolean isSupportedXZPlane() { // i.e. y is fixed
		return false;
	}
	

	//       |                  
	//       |                  
	//       |                  
	//       |   |-------|      
	//       |   |       |      
	//       |   |       |      
	//       |---x=========================
	//
	//       |                  
	//       |                  
	//       |         |-------|
	//       |         |       |
	//       |         |       |
	//       |         |       |
	//       |---------x===================
	//

	public abstract SimplePoint3D moveX(int x);

	//       |                  
	//       |                  
	//       |                  
	//       |   |-------|      
	//       |   |       |      
	//       |   |       |      
	//       |---x=========================
	//
	//       |      added y support        
	//       |         |         
	//       |---------║         
	//       |         ║         
	//       |         ║-------|
	//       |         ║       |
	//       |         ║       |
	//       |         ║       |
	//       |---------x===================
	//

	public abstract SimplePoint3D moveX(int x, Placement yzSupport);

	//
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║-------|      
	//       |   ║       |      
	//       |   ║       |      
	//       |---x-------|----------------
	//
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║---------|      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   x---------|      
	//       |                 
	//       |                 
	//       |                 
	//       |                 
	//       |---------------------------

	public abstract SimplePoint3D moveY(int y);

	//
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║-------|      
	//       |   ║       |      
	//       |   ║       |      
	//       |---x-------|----------------
	//
	//       |   ║              
	//       |   ║              
	//       |   ║              
	//       |   ║---------|      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   ║         |      
	//       |   x===================  <-- added xz support
	//       |                 
	//       |                 
	//       |                 
	//       |                 
	//       |---------------------------

	public abstract SimplePoint3D moveY(int y, Placement xzSupport);

	public abstract SimplePoint3D moveZ(int z);

	public abstract SimplePoint3D moveZ(int z, Placement xySupport);

	/**
	 * Return the moved point's support mask, with xy, xz and yz planes represented by bits 4, 2 and 1.
	 *
	 * @param x the x coordinate of the moved point
	 * @return the support mask after moving along x
	 */
	protected int getSupportMaskAfterMoveX(int x) {
		SimplePoint3D moved = moveX(x);
		return (moved.isSupportedXYPlane() ? 4 : 0) + (moved.isSupportedXZPlane() ? 2 : 0) + (moved.isSupportedYZPlane() ? 1 : 0);
	}

	/**
	 * Return the moved point's support mask, with xy, xz and yz planes represented by bits 4, 2 and 1.
	 *
	 * @param y the y coordinate of the moved point
	 * @return the support mask after moving along y
	 */
	protected int getSupportMaskAfterMoveY(int y) {
		SimplePoint3D moved = moveY(y);
		return (moved.isSupportedXYPlane() ? 4 : 0) + (moved.isSupportedXZPlane() ? 2 : 0) + (moved.isSupportedYZPlane() ? 1 : 0);
	}

	/**
	 * Return the moved point's support mask, with xy, xz and yz planes represented by bits 4, 2 and 1.
	 *
	 * @param z the z coordinate of the moved point
	 * @return the support mask after moving along z
	 */
	protected int getSupportMaskAfterMoveZ(int z) {
		SimplePoint3D moved = moveZ(z);
		return (moved.isSupportedXYPlane() ? 4 : 0) + (moved.isSupportedXZPlane() ? 2 : 0) + (moved.isSupportedYZPlane() ? 1 : 0);
	}
	
	public abstract SimplePoint3D copy(int maxX, int maxY, int maxZ);

	public abstract SimplePoint3D copy();
}
