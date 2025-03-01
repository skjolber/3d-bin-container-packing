package com.github.skjolber.packing.api;

import java.util.Arrays;
import java.util.List;

public class Box extends Stackable {

	private static final long serialVersionUID = 1L;

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

			return new Box(id, description, size.getVolume(), weight, getStackValues());
		}

		@SuppressWarnings("unchecked")
		protected <T> T[] newStackValueArray(int size) {
			return (T[])new BoxStackValue[size];
		}

		protected BoxStackValue newStackValue(int dx, int dy, int dz, StackValueConstraint constraint, List<Surface> surfaces) {
			return new BoxStackValue(dx, dy, dz, constraint, surfaces);
		}
	}

	protected final int weight;
	protected final BoxStackValue[] stackValues;
	protected final long volume;
	protected final long minimumArea;
	protected final long maximumArea;

	protected final String id;
	protected final String description;
	
	public Box(String id, String description, long volume, int weight, BoxStackValue[] stackValues) {
		this.id = id;
		this.description = description;
		
		this.volume = volume;
		this.weight = weight;
		this.stackValues = stackValues;

		this.minimumArea = getMinimumArea(stackValues);
		this.maximumArea = getMinimumArea(stackValues);
		
		for (BoxStackValue boxStackValue : stackValues) {
			boxStackValue.setStackable(this);
		}
	}
	
	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public int getWeight() {
		return weight;
	}

	@Override
	public BoxStackValue[] getStackValues() {
		return stackValues;
	}

	public long getVolume() {
		return volume;
	}

	@Override
	public Box clone() {
		BoxStackValue[] stackValues = new BoxStackValue[this.stackValues.length];
		for(int i = 0; i < stackValues.length; i++) {
			stackValues[i] = this.stackValues[i].clone();
		}
		return new Box(id, description, volume, weight, stackValues);
	}
	
	public StackValue getStackValue(int index) {
		return stackValues[index];
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
		return "Box " + (description != null ? description : "") + "[weight=" + weight + ", rotations=" + Arrays.toString(stackValues) + ", volume=" + volume + "]";
	}

}
