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
			
			return new Box(id, description, size.getVolume(), weight, getStackValues());
		}
		
		protected <T> T[] newStackValueArray(int size) {
			return (T[]) new BoxStackValue[size];
		}

		protected BoxStackValue newStackValue(int dx, int dy, int dz, StackConstraint constraint, List<Surface> surfaces) {
			return new BoxStackValue(dx, dy, dz, constraint, surfaces);
		}
	}
	
	protected final int weight;
	protected final BoxStackValue[] rotations;
	protected final long volume;
	protected final long minimumArea;
	protected final long maximumArea;

	public Box(String id, String name, long volume, int weight, BoxStackValue[] stackValues) {
		super(id, name);
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
		return new Box(id, description, volume, weight, rotations);
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
		return "Box " + (description != null ? description : "") + "[weight=" + weight + ", rotations=" + Arrays.toString(rotations) + ", volume=" + volume + "]";
	}
	
	
	
}
