package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;


/**
 * {@linkplain Stackable} builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

public class AbstractContainerBuilder<B extends AbstractContainerBuilder<B>> {

	public static class Rotation {
		
		protected int dx; // width
		protected int dy; // depth
		protected int dz; // height

		protected int maxLoadWeight;
	
		protected final int loadDx; // x
		protected final int loadDy; // y
		protected final int loadDz; // z
		
		protected final StackConstraint stackConstraint;

		protected long volume;
		protected long loadVolume;

		public Rotation(int dx, int dy, int dz, int loadDx, int loadDy, int loadDz, int maxLoadWeight, StackConstraint stackConstraint) {
			super();
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			this.stackConstraint = stackConstraint;
			
			this.loadDx = loadDx;
			this.loadDy = loadDx;
			this.loadDz = loadDz;

			this.maxLoadWeight = maxLoadWeight;
			
			this.volume = (long)dx * (long)dy * (long)dz;
			this.loadVolume = (long)loadDx * (long)loadDy * (long)loadDz;
		}

		public int getMaxLoadWeight() {
			return maxLoadWeight;
		}

		public int getLoadVolume() {
			return maxLoadWeight;
		}

		public long getVolume() {
			return volume;
		}

	}
	
	protected List<Rotation> rotations = new ArrayList<>();
	protected StackConstraint defaultConstraint;

	protected String name;
	
	public B withDefaultConstraint(StackConstraint stackConstraint) {
		this.defaultConstraint = stackConstraint;
		return (B)this;
	}
	
	public B withName(String name) {
		this.name = name;
		return (B)this;
	}

	public B withRotate(int dx, int dy, int dz, int loadDx, int loadDy, int loadDz, int maxLoadWeight, StackConstraint stackConstraint) {
		rotations.add(new Rotation(dx, dy, dz, loadDx, loadDy, loadDz, maxLoadWeight, stackConstraint));
		
		return (B)this;

	}

	public B withRotateXY(int dx, int dy, int dz, int loadDx, int loadDy, int loadDz, int maxLoadWeight, StackConstraint stackConstraint) {
		rotations.add(new Rotation(dx, dy, dz, loadDx, loadDy, loadDz, maxLoadWeight, stackConstraint));
		
		if(dx != dy) {
			rotations.add(new Rotation(dy, dx, dz, loadDy, loadDx, loadDz, maxLoadWeight, stackConstraint));
		}
		
		return (B)this;
	}
	
	public B withRotateXYZ(int dx, int dy, int dz, int loadDx, int loadDy, int loadDz, int maxLoadWeight, StackConstraint stackConstraint) {
		rotations.add(new Rotation(dx, dy, dz, loadDy, loadDx, loadDz, maxLoadWeight, stackConstraint));
		
		if(!(dx == dy && dx == dz)) {

			//
			//              dx
			// ---------------------------
			// |                         |
			// |                         | dy
			// |                         |
			// ---------------------------
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
			
			if(dx != dy) {
				rotations.add(new Rotation(dy, dx, dz, loadDy, loadDx, loadDz, maxLoadWeight, stackConstraint));
			}
			
			rotations.add(new Rotation(dx, dz, dy, loadDx, loadDz, loadDy, maxLoadWeight, stackConstraint));
			if(dx != dz) {
				rotations.add(new Rotation(dz, dx, dy, loadDz, loadDx, loadDy, maxLoadWeight, stackConstraint));
			}
			
			rotations.add(new Rotation(dy, dz, dx, loadDy, loadDz, loadDx, maxLoadWeight, stackConstraint));
			if(dy != dz) {
				rotations.add(new Rotation(dz, dy, dx, loadDz, loadDy, loadDx, maxLoadWeight, stackConstraint));
			}
		}			
		
		return (B)this;
	}

}
