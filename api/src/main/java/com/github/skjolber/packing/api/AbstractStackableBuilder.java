package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;


/**
 * {@linkplain Stackable} builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

@SuppressWarnings("unchecked")
public abstract class AbstractStackableBuilder<B extends AbstractStackableBuilder<B>> extends AbstractPhysicsBuilder<B> {

	protected String id;
	protected String description;

	public B withDescription(String description) {
		this.description = description;
		return (B)this;
	}

	public B withId(String id) {
		this.id = id;
		return (B)this;
	}
	
	protected <T> T[] getStackValues() {
		
		// z              y
		// |             / 
		// |            / 
		// |         /-------/|
		// |        /       / |
		// |       /       /  |    
		// |      /  top  /t  /
		// |     /   xy  /h  /  
	    // |    /       /g z/ 
		// |   /       /i y/
		// |  |-------|r  /      
		// |  |  xz   |  /
		// |  | front | /
		// |  |-------|/      
		// | /      
		// |/       
		// |------------------ x
		//

		
		List<BoxStackValue> list = new ArrayList<>();

		int dx = size.getDx();
		int dy = size.getDy();
		int dz = size.getDz();
		
		 // dx, dy, dz
		
		if(dx == dy && dx == dz) { // square 3d
			// all sides are equal

			// z          y
			// |         / 
			// |        / 
			// |     /-------/|
		    // |    / xy    / | 
			// |   /       /  |
			// |  |-------|yz /      
			// |  |       |  /
			// |  |   xz  | /
			// |  |-------|/      
			// | /      
			// |/       
			// |------------------ x
			//
			
			if(stackableSurface.is0() || stackableSurface.is90()) {
				list.add(newStackValue(dx, dy, dz, constraint, stackableSurface.getSides()));
			}
		} else if(dx == dy) {


			// z               y
			// |              / 
			// |             / 
			// |     /-------/|
		    // |    /  xy   / | 
			// |   /       /  |
			// |  |-------|   |      
			// |  |       |   |
			// |  |       | y |
			// |  |  xz   | z |
			// |  |       |   |
			// |  |       |   |
			// |  |       |  /
			// |  |       | /
			// |  |-------|/      
			// | /      
			// |/       
			// |------------------ x
			//

			// add xz/yz and xy
			
			if(stackableSurface.isXY()) {
				list.add(newStackValue(dx, dx, dz, constraint, stackableSurface.getXYSurfaces()));
			}
			if(stackableSurface.isXZ() || stackableSurface.isYZ()) {
				
				boolean zero = stackableSurface.isXZ0() || stackableSurface.isYZ0();
				boolean ninety = stackableSurface.isXZ90() || stackableSurface.isYZ90();
				
				if(zero) {
					list.add(newStackValue(dx, dz, dx, constraint, stackableSurface.getYZAndXZSurfaces0()));
				}					
				if(ninety) {
					list.add(newStackValue(dz, dx, dx, constraint, stackableSurface.getYZAndXZSurfaces90()));
					
				}
				
			}
		} else if(dz == dy) {

			
			// z           y
			// |          / 
			// |         / 
			// |     /--------------------/|
		    // |    /        xy          / | 
			// |   /                    /  |
			// |  |--------------------| yz/      
			// |  |         xz         |  /
			// |  |                    | /
			// |  |--------------------|/      
			// | /      
			// |/       
			// |----------------------------------- x
			//
			
			// add xz/xy and yz

			if(stackableSurface.isYZ()) {
				list.add(newStackValue(dy, dy, dx, constraint, stackableSurface.getYZSurfaces()));
			}
			if(stackableSurface.isXY() || stackableSurface.isXZ()) {
				
				boolean zero = stackableSurface.isXY0() || stackableSurface.isXZ0();
				boolean ninety = stackableSurface.isXY90() || stackableSurface.isXZ90();
				
				if(zero) {
					list.add(newStackValue(dx, dz, dz, constraint, stackableSurface.getXYAndXZSurfaces0()));
				}
				if(ninety) {
					list.add(newStackValue(dz, dx, dz, constraint, stackableSurface.getXYAndXZSurfaces90()));
				}
			}
			
		} else if(dx == dz) {
			
			// add xy/zy and xz

			//  
			// z               y
			// |              / 
			// |             / 
			// |         /-------/|
			// |        /       / |
			// |       /       /  |    
			// |      /  xy   /   /
			// |     /       /   /  
		    // |    /       / z / 
			// |   /       / y /
			// |  |-------|   /      
			// |  |       |  /
			// |  |  xz   | /
			// |  |-------|/      
			// | /      
			// |/       
			// |------------------ x
			//

			if(stackableSurface.isXZ()) {
				list.add(newStackValue(dx, dx, dy, constraint, stackableSurface.getXZSurfaces()));
			}
			if(stackableSurface.isXY() || stackableSurface.isYZ()) {
				boolean zero = stackableSurface.isXY0() || stackableSurface.isYZ0();
				boolean ninety = stackableSurface.isXY90() || stackableSurface.isYZ90();

				if(zero) {
					list.add(newStackValue(dx, dy, dx, constraint, stackableSurface.getXYAndYZSurfaces0()));
				}
				if(ninety) {
					list.add(newStackValue(dy, dx, dx, constraint, stackableSurface.getXYAndYZSurfaces90()));
				}
			}
		} else {
			// no equal length edges
			
			//
			//              dx
			// ---------------------------
			// |                         |
			// |                         | dy
			// |                         |
			// ---------------------------
			//
			//    dy
			// --------
			// |      |
			// |      |
			// |      |
			// |      |
			// |      |
			// |      | dz
			// |      |
			// |      |
			// |      |
			// |      |
			// |      |
			// --------
			//			
			//              dx
			// ---------------------------
			// |                         |
			// |                         |
			// |                         |
			// |                         | dz
			// |                         |
			// |                         |
			// --------------------------- 
			//
			//
			//    dy
			// ----------------
			// |              |
			// |              |
			// |              |
			// |              |
			// |              |
			// |              | dx
			// |              |
			// |              |
			// |              |
			// |              |
			// |              |
			// ----------------
			//			
			//			
			//    dy
			// --------
			// |      |
			// |      |
			// |      |
			// |      | dz
			// |      |
			// |      |
			// --------
			//
			//        dy
			// ----------------
			// |              |
			// |              | dz
			// |              |
			// ----------------
			//			
			
			if(stackableSurface.isXY0()) {
				list.add(newStackValue(dx, dy, dz, constraint, stackableSurface.getXY0Surfaces()));
			}					
			if(stackableSurface.isXY90()) {
				list.add(newStackValue(dy, dx, dz, constraint, stackableSurface.getXY90Surfaces()));
			}

			if(stackableSurface.isXZ0()) {
				list.add(newStackValue(dx, dz, dy, constraint, stackableSurface.getXZ0Surfaces()));
			}					
			if(stackableSurface.isXZ90()) {
				list.add(newStackValue(dz, dx, dy, constraint, stackableSurface.getXZ90Surfaces()));
			}

			if(stackableSurface.isYZ0()) {
				list.add(newStackValue(dz, dy, dx, constraint, stackableSurface.getYZ0Surfaces()));
			}					
			if(stackableSurface.isYZ90()) {
				list.add(newStackValue(dy, dz, dx, constraint, stackableSurface.getYZ90Surfaces()));
			}
		}

		if(list.isEmpty()) {
			throw new IllegalStateException("Expected at least one stackable surface");
		}
		return list.toArray(newStackValueArray(list.size()));
	}

	
	protected abstract <T> T[] newStackValueArray(int size);

	protected abstract BoxStackValue newStackValue(int dx, int dy, int dz, StackConstraint constraint, List<Surface> surfaces);
	
}
