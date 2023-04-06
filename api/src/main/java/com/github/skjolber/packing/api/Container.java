package com.github.skjolber.packing.api;

import java.math.BigDecimal;

public abstract class Container extends Stackable {

	private static final long serialVersionUID = 1L;

	public static Builder newBuilder() {
		return new Builder();
	}

	protected static BigDecimal getMaxLoadWeight(ContainerStackValue[] values) {
		BigDecimal maxLoadWeight = BigDecimal.valueOf(-1);

		for (ContainerStackValue value : values) {
			if(value.getMaxLoadWeight().compareTo(maxLoadWeight) > 0) {
				maxLoadWeight = value.getMaxLoadWeight();
			}
		}
		return maxLoadWeight;
	}

	protected static BigDecimal calculateMinimumArea(StackValue[] values) {
		BigDecimal minimumArea = BigDecimal.valueOf(Long.MAX_VALUE);
		for (StackValue boxStackValue : values) {
			if(minimumArea.compareTo(boxStackValue.getArea()) > 0) {
				minimumArea = boxStackValue.getArea();
			}
		}
		return minimumArea;
	}

	protected static BigDecimal getMaxLoadVolume(ContainerStackValue[] values) {
		BigDecimal maxLoadVolume = BigDecimal.valueOf(-1);

		for (ContainerStackValue value : values) {
			if(value.getMaxLoadVolume().compareTo(maxLoadVolume) > 0) {
				maxLoadVolume = value.getMaxLoadVolume();
			}
		}
		return maxLoadVolume;
	}

	public static class Builder extends AbstractContainerBuilder<Builder> {

		protected BigDecimal emptyWeight = BigDecimal.valueOf(-1);
		protected Stack stack;
		protected boolean fixed = false;

		public Builder withFixed(boolean fixed) {
			this.fixed = fixed;
			return this;
		}

		public Builder withFixedStack(Stack stack) {
			this.stack = stack;
			this.fixed = true;
			return this;
		}

		public Builder withStack(Stack stack) {
			this.stack = stack;
			return this;
		}

		public Builder withEmptyWeight(BigDecimal emptyWeight) {
			this.emptyWeight = emptyWeight;

			return this;
		}

		public DefaultContainer build() {
			if(dx.compareTo(BigDecimal.valueOf(-1)) == 0) {
				throw new IllegalStateException("Expected size");
			}
			if(dy.compareTo(BigDecimal.valueOf(-1)) == 0) {
				throw new IllegalStateException("Expected size");
			}
			if(dz.compareTo(BigDecimal.valueOf(-1)) == 0) {
				throw new IllegalStateException("Expected size");
			}
			if(maxLoadWeight.compareTo(BigDecimal.valueOf(-1)) == 0) {
				throw new IllegalStateException("Expected max weight");
			}
			if(loadDx.compareTo(BigDecimal.valueOf(-1)) == 0) {
				loadDx = dx;
			}
			if(loadDy.compareTo(BigDecimal.valueOf(-1)) == 0) {
				loadDy = dy;
			}
			if(loadDz.compareTo(BigDecimal.valueOf(-1)) == 0) {
				loadDz = dz;
			}
			if(surfaces == null || surfaces.isEmpty()) {
				surfaces = Surface.DEFAULT_SURFACE;
			}

			if(emptyWeight.compareTo(BigDecimal.valueOf(-1)) == 0) {
				throw new IllegalStateException("Expected empty weight");
			}
			if(stack == null) {
				stack = new DefaultStack();
			}

			BigDecimal volume = dx.multiply(dy).multiply(dz);

			return new DefaultContainer(id, description, volume, emptyWeight, getStackValues(), stack);
		}

		protected ContainerStackValue[] getStackValues() {
			if(fixed) {
				FixedContainerStackValue[] stackValues = new FixedContainerStackValue[1];

				BigDecimal stackWeight = stack.getWeight();

				stackValues[0] = new FixedContainerStackValue(
						dx, dy, dz,
						stackConstraint,
						stackWeight, emptyWeight,
						loadDx, loadDy, loadDz,
						maxLoadWeight,
						surfaces);

				return stackValues;
			} else {
				DefaultContainerStackValue[] stackValues = new DefaultContainerStackValue[1];

				stackValues[0] = new DefaultContainerStackValue(
						dx, dy, dz,
						stackConstraint,
						loadDx, loadDy, loadDz,
						maxLoadWeight,
						surfaces);

				return stackValues;
			}
		}

	}

	protected final BigDecimal emptyWeight;
	/** i.e. best of the stack values */
	protected final BigDecimal maxLoadVolume;
	/** i.e. best of the stack values */
	protected final BigDecimal maxLoadWeight;

	protected final BigDecimal volume;
	protected final BigDecimal minArea;
	protected final BigDecimal maxArea;

	public Container(String id, String name, BigDecimal volume, BigDecimal emptyWeight, BigDecimal maxLoadVolume,
					 BigDecimal maxLoadWeight, BigDecimal minArea, BigDecimal maxArea) {
		super(id, name);

		this.emptyWeight = emptyWeight;
		this.maxLoadVolume = maxLoadVolume;
		this.maxLoadWeight = maxLoadWeight;

		this.volume = volume;
		this.minArea = minArea;
		this.maxArea = maxArea;
	}

	@Override
	public BigDecimal getWeight() {
		return emptyWeight.add(getStack().getWeight());
	}

	public BigDecimal getMaxLoadVolume() {
		return maxLoadVolume;
	}

	public BigDecimal getMaxLoadWeight() {
		return maxLoadWeight;
	}

	@Override
	public abstract ContainerStackValue[] getStackValues();

	public abstract Stack getStack();

	public BigDecimal getEmptyWeight() {
		return emptyWeight;
	}

	public BigDecimal getMaxWeight() {
		return emptyWeight.add(maxLoadWeight);
	}

	public abstract boolean canLoad(Stackable box);

	@Override
	public abstract Container clone();

	public BigDecimal getLoadWeight() {
		return getStack().getWeight();
	}

	@Override
	public BigDecimal getVolume() {
		return volume;
	}

	@Override
	public BigDecimal getMinimumArea() {
		return minArea;
	}

	@Override
	public BigDecimal getMaximumArea() {
		return maxArea;
	}

	public BigDecimal getLoadVolume() {
		return getStack().getVolume();
	}
}
