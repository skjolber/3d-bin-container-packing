package com.github.skjolber.packing.ep.points2d;

import java.io.Serializable;

import com.github.skjolber.packing.api.StackPlacement;

public abstract class SimplePoint2D extends Point2D {

	public SimplePoint2D(int minX, int minY, int maxX, int maxY) {
		super(minX, minY, maxX, maxY);
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

	public abstract SimplePoint2D moveX(int x);

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

	public abstract SimplePoint2D moveX(int x, StackPlacement ySupport);

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

	public abstract SimplePoint2D moveY(int y);

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
	//       |   x===================  <-- added x support
	//       |                 
	//       |                 
	//       |                 
	//       |                 
	//       |---------------------------

	public abstract SimplePoint2D moveY(int y, StackPlacement xSupport);

}
