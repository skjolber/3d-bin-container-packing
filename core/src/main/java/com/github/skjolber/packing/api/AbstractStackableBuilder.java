package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.AbstractStackableBuilder.Rotation;


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
		
		protected long volume;

		public Rotation(int dx, int dy, int dz, StackConstraint stackConstraint) {
			super();
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			this.stackConstraint = stackConstraint;
			
			this.volume = (long)dx * (long)dy * (long)dz;;
		}
		
		public long getVolume() {
			return volume;
		}
	}
	
	protected List<Rotation> rotations = new ArrayList<>();
	
	protected String name;

	protected StackConstraint defaultConstraint;
	
	protected long volume = -1L;
	
	public B withName(String name) {
		this.name = name;
		return (B)this;
	}

	public B withRotate(Rotation rotation) {
		add(rotation);
		
		return (B)this;
	}
	
	protected void add(Rotation rotation) {
		if(!rotations.isEmpty()) {
			if(rotations.get(0).getVolume() != rotation.getVolume()) {
				throw new IllegalStateException("Expected equal volume for all rotations");
			}
		}
		rotations.add(rotation);
	}
	
	public B withRotate(int dx, int dy, int dz) {
		return withRotate(new Rotation(dx, dy, dz, null));
	}

	public B withRotate(int dx, int dy, int dz, StackConstraint stackConstraint) {
		return withRotate(new Rotation(dx, dy, dz, stackConstraint));
	}
	
	public B withRotateXY(int dx, int dy, int dz) {
		return withRotateXY(dx, dy, dz, null);
	}

	public B withDefaultConstraint(StackConstraint stackConstraint) {
		this.defaultConstraint = stackConstraint;
		return (B)this;
	}

	public B withRotateXY(int dx, int dy, int dz, StackConstraint stackConstraint) {
		add(new Rotation(dx, dy, dz, stackConstraint));
		
		if(dx != dy) {
			add(new Rotation(dy, dx, dz, stackConstraint));
		}
		
		return (B)this;
	}

	public B withRotateXYZ(int dx, int dy, int dz) {
		return withRotateXYZ(dx, dy, dz, null);
	}

	public B withRotateXYZ(int dx, int dy, int dz, StackConstraint stackConstraint) {
		add(new Rotation(dx, dy, dz, stackConstraint));
		
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
			
			add(new Rotation(dz, dx, dy, stackConstraint));
			add(new Rotation(dy, dz, dx, stackConstraint));
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
			
			add(new Rotation(dy, dx, dz, stackConstraint));
			
			add(new Rotation(dx, dz, dy, stackConstraint));
			add(new Rotation(dz, dx, dy, stackConstraint));
			
			add(new Rotation(dy, dz, dx, stackConstraint));
			add(new Rotation(dz, dy, dx, stackConstraint));
		}			
		
		return (B)this;
	}
	
}
