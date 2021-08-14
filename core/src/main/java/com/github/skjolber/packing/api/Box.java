package com.github.skjolber.packing.api;

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

		public Box build(Box box) {
			if(rotations.isEmpty()) {
				throw new IllegalStateException("No rotations");
			}
			if(weight == null) {
				throw new IllegalStateException("No weight");
			}
			
			return new Box(this);
		}
		
		protected BoxStackValue[] getStackValues(Box box) {
			BoxStackValue[] stackValues = new BoxStackValue[rotations.size()];
			
			for (int i = 0; i < rotations.size(); i++) {
				Rotation rotation = rotations.get(i);
				
				StackConstraint constraint = rotation.stackConstraint != null ? rotation.stackConstraint : defaultConstraint;

				stackValues[i] = new BoxStackValue(rotation.dx, rotation.dy, rotation.dz, box, constraint);
			}	
			return stackValues;
		}
		
	}
	
	protected final int weight;
	protected final BoxStackValue[] rotations;
	protected final long volume;

	public Box(Builder builder) {
		super(builder.name);
		
		this.weight = builder.weight;
		this.rotations = builder.getStackValues(this);
		this.volume = rotations[0].volume;
	}

	protected Box(String name, long volume, int weight, BoxStackValue[] rotations) {
		super(name);
		this.volume = volume;
		this.weight = weight;
		this.rotations = rotations;
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
	
}
