package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Box extends Stackable {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractStackableBuilder<Builder> {
		
		protected Integer weight;
		
		public Builder withWeight(int weight) {
			this.weight = weight;
			
			return this;
		}

		public Box build() {
			if(size == null) {
				throw new IllegalStateException("No size");
			}
			if(weight == null) {
				throw new IllegalStateException("No weight");
			}

			if(stackableSurface == null) {
				stackableSurface = StackableSurface.TWO_D;
			}
			
			return new Box(name, size.getVolume(), weight, getStackValues());
		}
		
		protected BoxStackValue[] getStackValues() {
			
			List<BoxStackValue> list = new ArrayList<>();

			int dx = size.getWidth();
			int dy = size.getDepth();
			int dz = size.getHeight();
			
			 // dx, dy, dz
			
			if(dx == dy && dx == dz) { // square 3d
				// all sides are equal

				if(stackableSurface.is0() || stackableSurface.is90()) {
					list.add(new BoxStackValue(dx, dy, dz, constraint, stackableSurface.getSides()));
				}
			} else if(dx == dy) {
				
				// add xz/yz and xy

				if(stackableSurface.isXY()) {
					list.add(new BoxStackValue(dx, dx, dz, constraint, stackableSurface.getXYSurfaces()));
				}
				if(stackableSurface.isXZ() || stackableSurface.isYZ()) {
					
					boolean zero = stackableSurface.isXZ0() || stackableSurface.isYZ0();
					boolean ninety = stackableSurface.isXZ90() || stackableSurface.isYZ90();
					
					if(zero) {
						list.add(new BoxStackValue(dx, dz, dx, constraint, stackableSurface.getYZAndXZSurfaces0()));
					}					
					if(ninety) {
						list.add(new BoxStackValue(dz, dx, dx, constraint, stackableSurface.getYZAndXZSurfaces90()));
						
					}
					
				}
			} else if(dz == dy) {

				// add xz/xy and yz

				if(stackableSurface.isYZ()) {
					list.add(new BoxStackValue(dy, dy, dx, constraint, stackableSurface.getYZSurfaces()));
				}
				if(stackableSurface.isXY() || stackableSurface.isXZ()) {
					
					boolean zero = stackableSurface.isXY0() || stackableSurface.isXZ0();
					boolean ninety = stackableSurface.isXY90() || stackableSurface.isXZ90();
					
					if(zero) {
						list.add(new BoxStackValue(dz, dx, dz, constraint, stackableSurface.getXYAndXZSurfaces0()));
					}
					if(ninety) {
						list.add(new BoxStackValue(dx, dz, dz, constraint, stackableSurface.getXYAndXZSurfaces90()));
					}
				}
				
			} else if(dx == dz) {
				
				// add xy/zy and xz

				if(stackableSurface.isXZ()) {
					list.add(new BoxStackValue(dx, dx, dy, constraint, stackableSurface.getXZSurfaces()));
				}
				if(stackableSurface.isXY() || stackableSurface.isYZ()) {
					boolean zero = stackableSurface.isXY0() || stackableSurface.isYZ0();
					boolean ninety = stackableSurface.isXY90() || stackableSurface.isYZ90();

					if(zero) {
						list.add(new BoxStackValue(dx, dy, dx, constraint, stackableSurface.getXYAndYZSurfaces0()));
					}
					if(ninety) {
						list.add(new BoxStackValue(dy, dx, dx, constraint, stackableSurface.getXYAndYZSurfaces90()));
					}
					
					
				}
			} else {
				// not equal length edges
				
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
				
				add(new Layout(dx, dy, dz, stackConstraint));
				add(new Layout(dy, dx, dz, stackConstraint));
				
				add(new Layout(dx, dz, dy, stackConstraint));
				add(new Layout(dz, dx, dy, stackConstraint));
				
				add(new Layout(dy, dz, dx, stackConstraint));
				add(new Layout(dz, dy, dx, stackConstraint));
			}

			if(list.isEmpty()) {
				throw new IllegalStateException("Expected at least one stackable surface");
			}
		
			BoxStackValue[] stackValues = new BoxStackValue[placements.size()];
			
			for (int i = 0; i < placements.size(); i++) {
				Layout rotation = placements.get(i);
				
				StackConstraint constraint = rotation.stackConstraint != null ? rotation.stackConstraint : defaultConstraint;

				stackValues[i] = new BoxStackValue(rotation.dx, rotation.dy, rotation.dz, constraint);
			}	
			return stackValues;
		}
		
	}
	
	protected final int weight;
	protected final BoxStackValue[] rotations;
	protected final long volume;
	protected final long minimumArea;
	protected final long maximumArea;

	protected Box(String name, long volume, int weight, BoxStackValue[] stackValues) {
		super(name);
		this.volume = volume;
		this.weight = weight;
		this.rotations = stackValues;
		
		this.minimumArea = getMinimumArea(stackValues);
		this.maximumArea = getMinimumArea(stackValues);
	}

	@Override
	public int getWeight() {
		return weight;
	}

	@Override
	public BoxStackValue[] getStackValues() {
		return rotations;
	}

	public long getVolume() {
		return volume;
	}

	@Override
	public Box clone() {
		return new Box(name, volume, weight, rotations);
	}
	
	@Override
	public long getMinimumArea() {
		return minimumArea;
	}
	
	@Override
	public long getMaximumArea() {
		return maximumArea;
	}

	@Override
	public String toString() {
		return "Box " + (name != null ? name : "") + "[weight=" + weight + ", rotations=" + Arrays.toString(rotations) + ", volume=" + volume + "]";
	}
	
	
	
}
