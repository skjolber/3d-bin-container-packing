package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;


/**
 * {@linkplain Stackable} builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

public class AbstractStackableBuilder<B extends AbstractStackableBuilder<B>> {

	public static class Rotation {
		
		protected int dx; // width
		protected int dy; // depth
		protected int dz; // height
		
		protected StackConstraint stackConstraint;

		public Rotation(int dx, int dy, int dz, StackConstraint stackConstraint) {
			super();
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			this.stackConstraint = stackConstraint;
		}
	}
	
	protected List<Rotation> rotations = new ArrayList<>();
	
	protected String name;

	protected StackConstraint defaultConstraint;
	
	public B withName(String name) {
		this.name = name;
		return (B)this;
	}

	public B withRotate(int dx, int dy, int dz) {
		rotations.add(new Rotation(dx, dy, dz, null));
		
		return (B)this;
	}

	public B withRotate(int dx, int dy, int dz, StackConstraint stackConstraint) {
		rotations.add(new Rotation(dx, dy, dz, stackConstraint));
		
		return (B)this;
	}
	
	public B withRotateXY(int dx, int dy, int dz) {
		return withRotateXY(dx, dy, dz, null);
	}

	public B withDefaultConstraint(StackConstraint stackConstraint) {
		this.defaultConstraint = stackConstraint;
		return (B)this;
	}

	
	public B withRotateXY(int dx, int dy, int dz, StackConstraint stackConstraint) {
		rotations.add(new Rotation(dx, dy, dz, stackConstraint));
		
		if(dx != dy) {
			rotations.add(new Rotation(dy, dx, dz, stackConstraint));
		}
		
		return (B)this;
	}

	public B withRotateXYZ(int dx, int dy, int dz) {
		return withRotateXYZ(dx, dy, dz, null);
	}

	public B withRotateXYZ(int dx, int dy, int dz, StackConstraint stackConstraint) {
		rotations.add(new Rotation(dx, dy, dz, stackConstraint));
		
		if(dx == dy && dx == dz) { // square 3d
			// all sides are equal
		} else if(dx == dy || dz == dy || dx == dz) {
			// one side is equal
			//
			//     dx
			// ---------
			// |       |
			// |       | dy
			// |       |
			// ---------
			//
			//              dz
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
			
			rotations.add(new Rotation(dz, dx, dy, stackConstraint));
			rotations.add(new Rotation(dy, dz, dx, stackConstraint));
		} else {
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
			
			rotations.add(new Rotation(dy, dx, dz, stackConstraint));
			
			rotations.add(new Rotation(dx, dz, dy, stackConstraint));
			rotations.add(new Rotation(dz, dx, dy, stackConstraint));
			
			rotations.add(new Rotation(dy, dz, dx, stackConstraint));
			rotations.add(new Rotation(dz, dy, dx, stackConstraint));
		}			
		
		return (B)this;
	}

}
