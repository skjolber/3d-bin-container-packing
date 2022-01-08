package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.Collections;
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

		protected List<Surface> surfaces;

		public Rotation(int dx, int dy, int dz, int loadDx, int loadDy, int loadDz, int maxLoadWeight, StackConstraint stackConstraint, List<Surface> surfaces) {
			super();
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			this.stackConstraint = stackConstraint;
			
			this.loadDx = loadDx;
			this.loadDy = loadDy;
			this.loadDz = loadDz;

			this.maxLoadWeight = maxLoadWeight;
			this.surfaces = surfaces;
			
			this.volume = (long)dx * (long)dy * (long)dz;
			this.loadVolume = (long)loadDx * (long)loadDy * (long)loadDz;
		}
		
		public List<Surface> getSurfaces() {
			return surfaces;
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

		public long getArea() {
			return dx * dy;
		}

	}
	
	protected List<Rotation> rotations = new ArrayList<>();
	protected StackConstraint defaultConstraint;

	protected String id;
	protected String description;
	
	public B withDefaultConstraint(StackConstraint stackConstraint) {
		this.defaultConstraint = stackConstraint;
		return (B)this;
	}
	
	public B withDescription(String description) {
		this.description = description;
		return (B)this;
	}
	
	public B withId(String id) {
		this.id = id;
		return (B)this;
	}

	public B withRotate(int dx, int dy, int dz, int loadDx, int loadDy, int loadDz, int maxLoadWeight, StackConstraint stackConstraint) {
		return withRotate(dx, dy, dz, loadDx, loadDy, loadDz, maxLoadWeight, stackConstraint, Collections.emptyList());
	}

	public B withRotate(int dx, int dy, int dz, int loadDx, int loadDy, int loadDz, int maxLoadWeight, StackConstraint stackConstraint, List<Surface> surfaces) {
		rotations.add(new Rotation(dx, dy, dz, loadDx, loadDy, loadDz, maxLoadWeight, stackConstraint, surfaces));
		
		return (B)this;

	}

	public B withRotateXY(int dx, int dy, int dz, int loadDx, int loadDy, int loadDz, int maxLoadWeight, StackConstraint stackConstraint) {
		return withRotateXY(dx, dy, dz, loadDx, loadDy, loadDz, maxLoadWeight, stackConstraint, Collections.emptyList());
	}

	public B withRotateXY(int dx, int dy, int dz, int loadDx, int loadDy, int loadDz, int maxLoadWeight, StackConstraint stackConstraint, List<Surface> surfaces) {
		rotations.add(new Rotation(dx, dy, dz, loadDx, loadDy, loadDz, maxLoadWeight, stackConstraint, surfaces));
		
		if(dx != dy) {
			rotations.add(new Rotation(dy, dx, dz, loadDy, loadDx, loadDz, maxLoadWeight, stackConstraint, surfaces));
		}
		
		return (B)this;
	}

	public B withRotateXYZ(int dx, int dy, int dz, int loadDx, int loadDy, int loadDz, int maxLoadWeight, StackConstraint stackConstraint) {
		return withRotateXYZ(dx, dy, dz, loadDx, loadDy, loadDz, maxLoadWeight, stackConstraint, Collections.emptyList());
	}

	public B withRotateXYZ(int dx, int dy, int dz, int loadDx, int loadDy, int loadDz, int maxLoadWeight, StackConstraint stackConstraint, List<Surface> surfaces) {
		rotations.add(new Rotation(dx, dy, dz, loadDy, loadDx, loadDz, maxLoadWeight, stackConstraint, surfaces));
		
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
				rotations.add(new Rotation(dy, dx, dz, loadDy, loadDx, loadDz, maxLoadWeight, stackConstraint, surfaces));
			}
			
			rotations.add(new Rotation(dx, dz, dy, loadDx, loadDz, loadDy, maxLoadWeight, stackConstraint, surfaces));
			if(dx != dz) {
				rotations.add(new Rotation(dz, dx, dy, loadDz, loadDx, loadDy, maxLoadWeight, stackConstraint, surfaces));
			}
			
			rotations.add(new Rotation(dy, dz, dx, loadDy, loadDz, loadDx, maxLoadWeight, stackConstraint, surfaces));
			if(dy != dz) {
				rotations.add(new Rotation(dz, dy, dx, loadDz, loadDy, loadDx, maxLoadWeight, stackConstraint, surfaces));
			}
		}			
		
		return (B)this;
	}

}
