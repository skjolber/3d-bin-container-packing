package com.github.skjolber.packing.api;

import java.util.Arrays;

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
			if(rotations.isEmpty()) {
				throw new IllegalStateException("No rotations");
			}
			if(weight == null) {
				throw new IllegalStateException("No weight");
			}
			
			Rotation rotation = rotations.get(0);
			long volume = rotation.getVolume();
			for(int i = 1; i < rotations.size(); i++) {
				if(rotations.get(i).getVolume() != volume) {
					throw new IllegalStateException();
				}
			}
			
			return new Box(name, volume, weight, getStackValues());
		}
		
		protected BoxStackValue[] getStackValues() {
			BoxStackValue[] stackValues = new BoxStackValue[rotations.size()];
			
			for (int i = 0; i < rotations.size(); i++) {
				Rotation rotation = rotations.get(i);
				
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
		return "Box [weight=" + weight + ", rotations=" + Arrays.toString(rotations) + ", volume=" + volume + "]";
	}
	
	
	
}
