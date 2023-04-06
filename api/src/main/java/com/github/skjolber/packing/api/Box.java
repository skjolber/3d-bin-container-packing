package com.github.skjolber.packing.api;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class Box extends Stackable {

	private static final long serialVersionUID = 1L;

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractStackableBuilder<Builder> {

		protected BigDecimal weight;

		public Builder withWeight(BigDecimal weight) {
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

		protected BoxStackValue newStackValue(BigDecimal dx, BigDecimal dy, BigDecimal dz, StackConstraint constraint, List<Surface> surfaces) {
			return new BoxStackValue(dx, dy, dz, constraint, surfaces);
		}
	}

	protected final BigDecimal weight;
	protected final BoxStackValue[] stackValues;
	protected final BigDecimal volume;
	protected final BigDecimal minimumArea;
	protected final BigDecimal maximumArea;

	public Box(String id, String name, BigDecimal volume, BigDecimal weight, BoxStackValue[] stackValues) {
		super(id, name);
		this.volume = volume;
		this.weight = weight;
		this.stackValues = stackValues;

		this.minimumArea = getMinimumArea(stackValues);
		this.maximumArea = getMinimumArea(stackValues);
	}

	@Override
	public BigDecimal getWeight() {
		return weight;
	}

	@Override
	public BoxStackValue[] getStackValues() {
		return stackValues;
	}

	public BigDecimal getVolume() {
		return volume;
	}

	@Override
	public Box clone() {
		return new Box(id, description, volume, weight, stackValues);
	}

	@Override
	public BigDecimal getMinimumArea() {
		return minimumArea;
	}

	@Override
	public BigDecimal getMaximumArea() {
		return maximumArea;
	}

	@Override
	public String toString() {
		return "Box " + (description != null ? description : "") + "[weight=" + weight + ", rotations=" + Arrays.toString(stackValues) + ", volume=" + volume + "]";
	}

}
