package com.github.skjolber.packing.api;

public class Container extends Stackable {

	private static final long serialVersionUID = 1L;

	public static Builder newBuilder() {
		return new Builder();
	}

	protected static int getMaxLoadWeight(ContainerStackValue[] values) {
		int maxLoadWeight = -1;

		for (ContainerStackValue value : values) {
			if(value.getMaxLoadWeight() > maxLoadWeight) {
				maxLoadWeight = value.getMaxLoadWeight();
			}
		}
		return maxLoadWeight;
	}

	protected static long calculateMinimumArea(StackValue[] values) {
		long minimumArea = Long.MAX_VALUE;
		for (StackValue boxStackValue : values) {
			if(minimumArea > boxStackValue.getArea()) {
				minimumArea = boxStackValue.getArea();
			}
		}
		return minimumArea;
	}

	protected static long getMaxLoadVolume(ContainerStackValue[] values) {
		long maxLoadVolume = -1;

		for (ContainerStackValue value : values) {
			if(value.getMaxLoadVolume() > maxLoadVolume) {
				maxLoadVolume = value.getMaxLoadVolume();
			}
		}
		return maxLoadVolume;
	}

	public static class Builder extends AbstractContainerBuilder<Builder> {

		protected int emptyWeight = -1;
		protected Stack stack;

		public Builder withFixedStack(Stack stack) {
			this.stack = stack;
			return this;
		}

		public Builder withStack(Stack stack) {
			this.stack = stack;
			return this;
		}

		public Builder withEmptyWeight(int emptyWeight) {
			this.emptyWeight = emptyWeight;

			return this;
		}

		public Container build() {
			if(dx == -1) {
				throw new IllegalStateException("Expected size");
			}
			if(dy == -1) {
				throw new IllegalStateException("Expected size");
			}
			if(dz == -1) {
				throw new IllegalStateException("Expected size");
			}
			if(maxLoadWeight == -1) {
				throw new IllegalStateException("Expected max weight");
			}
			if(loadDx == -1) {
				loadDx = dx;
			}
			if(loadDy == -1) {
				loadDy = dy;
			}
			if(loadDz == -1) {
				loadDz = dz;
			}
			if(surfaces == null || surfaces.isEmpty()) {
				surfaces = Surface.DEFAULT_SURFACE;
			}

			if(emptyWeight == -1) {
				throw new IllegalStateException("Expected empty weight");
			}
			if(stack == null) {
				stack = new DefaultStack();
			}

			long volume = (long)dx * (long)dy * (long)dz;

			return new Container(id, description, volume, emptyWeight, getStackValues(), stack);
		}

		protected ContainerStackValue[] getStackValues() {
			ContainerStackValue[] stackValues = new ContainerStackValue[1];

			stackValues[0] = new ContainerStackValue(
					dx, dy, dz,
					stackConstraint,
					loadDx, loadDy, loadDz,
					maxLoadWeight,
					surfaces);

			return stackValues;
		}

	}

	protected final long volume;

	protected final int emptyWeight;
	/** i.e. best of the stack values */
	protected final long maxLoadVolume;
	/** i.e. best of the stack values */
	protected final int maxLoadWeight;

	protected final ContainerStackValue minimumArea;
	protected final ContainerStackValue maximumArea;
	
	protected long minimumPressure;
	protected long maximumPressure;

	protected final String id;
	protected final String description;

	protected final ContainerStackValue[] stackValues;
	protected final Stack stack;

	public Container(String id, String description, long volume, int emptyWeight, ContainerStackValue[] stackValues, Stack stack) {
		this.id = id;
		this.description = description;
		
		this.emptyWeight = emptyWeight;
		
		this.stackValues = stackValues;

		this.maxLoadVolume = getMaxLoadVolume(stackValues);
		this.maxLoadWeight = getMaxLoadWeight(stackValues);
		
		this.minimumArea = (ContainerStackValue) getMinimumArea(stackValues);
		this.maximumArea = (ContainerStackValue) getMinimumArea(stackValues);
		
		this.minimumPressure = ( (emptyWeight + maxLoadWeight) * 1000L) / maximumArea.getArea();
		this.maximumPressure = ( (emptyWeight + maxLoadWeight) * 1000L) / minimumArea.getArea();

		this.volume = volume;
		
		this.stack = stack;
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
		return emptyWeight + stack.getWeight();
	}

	public long getMaxLoadVolume() {
		return maxLoadVolume;
	}

	public int getMaxLoadWeight() {
		return maxLoadWeight;
	}

	public int getEmptyWeight() {
		return emptyWeight;
	}

	public int getMaxWeight() {
		return emptyWeight + maxLoadWeight;
	}

	public int getLoadWeight() {
		return stack.getWeight();
	}

	@Override
	public long getVolume() {
		return volume;
	}

	@Override
	public long getMinimumArea() {
		return minimumArea.getArea();
	}

	@Override
	public long getMaximumArea() {
		return maximumArea.getArea();
	}

	public long getLoadVolume() {
		return stack.getVolume();
	}

	@Override
	public ContainerStackValue[] getStackValues() {
		return stackValues;
	}

	public ContainerStackValue getStackValue(int index) {
		return stackValues[index];
	}

	public boolean canLoad(Stackable stackable) {
		if(stackable.getVolume() > maxLoadVolume) {
			return false;
		}
		if(stackable.getWeight() > maxLoadWeight) {
			return false;
		}
		for (ContainerStackValue stackValue : stackValues) {
			if(stackValue.canLoad(stackable)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public Container clone() {
		ContainerStackValue[] stackValues = new ContainerStackValue[this.stackValues.length];
		for(int i = 0; i < stackValues.length; i++) {
			stackValues[i] = this.stackValues[i].clone();
		}
		return new Container(id, description, volume, emptyWeight, stackValues, new DefaultStack());
	}

	@Override
	public long getMinimumPressure() {
		return minimumPressure;
	}
	
	@Override
	public long getMaximumPressure() {
		return maximumPressure;
	}
}
