package com.github.skjolber.packing.ep.points3d;

import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.Point;

public abstract class SimplePoint3D extends Point {
	
	private static final long serialVersionUID = 1L;

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

	public abstract SimplePoint3D moveX(int x, StackPlacement yzSupport);

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

	public abstract SimplePoint3D moveY(int y, StackPlacement xzSupport);

	public abstract SimplePoint3D moveZ(int z);

	public abstract SimplePoint3D moveZ(int z, StackPlacement xySupport);
	
	public abstract SimplePoint3D clone(int maxX, int maxY, int maxZ);

	public abstract long calculateXYSupport(int dx, int dy);

	public abstract long calculateXZSupport(int dx, int dz);

	public abstract long calculateYZSupport(int dy, int dz);

}
