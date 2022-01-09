package com.github.skjolber.packing.api;

public class StackablePhysics {
	
	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {

		protected StackConstraint constraint;
		protected Dimension size;
		protected StackableSurface stackableSurface;

		public Builder withSize(int dx, int dy, int dz) {
			this.size = new Dimension(dx, dy, dz);
			
			return this;
		}

		public Builder withRotation2D() {
			stackableSurface = StackableSurface.TWO_D;
			return this;
		}

		public Builder withRotate3D() {
			stackableSurface = StackableSurface.THREE_D;
			return this;
		}
		
		public Builder withRotation(StackableSurface rotation) {
			this.stackableSurface = rotation;
			
			return this;
		}

		public Builder withConstraint(StackConstraint stackConstraint) {
			this.constraint = stackConstraint;
			return this;
		}
		
		public StackablePhysics build() {
			return new StackablePhysics(size, stackableSurface, constraint);
		}
	}

	protected StackConstraint constraint;
	protected Dimension size;
	protected StackableSurface rotation;

	public StackablePhysics(Dimension size, StackableSurface rotation, StackConstraint constraint) {
		super();
		this.size = size;
		this.rotation = rotation;
		this.constraint = constraint;
	}

	public StackConstraint getConstraint() {
		return constraint;
	}
	
	public StackableSurface getRotation() {
		return rotation;
	}
	
	public Dimension getSize() {
		return size;
	}
}
