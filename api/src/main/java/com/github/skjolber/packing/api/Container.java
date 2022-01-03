package com.github.skjolber.packing.api;

public abstract class Container extends Stackable {

	public static Builder newBuilder() {
		return new Builder();
	}
	
	protected static int getMaxLoadWeight(ContainerStackValue[] values) {
		int maxLoadWeight = -1;
		
		for(ContainerStackValue value : values) {
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
		
		for(ContainerStackValue value : values) {
			if(value.getMaxLoadVolume() > maxLoadVolume) {
				maxLoadVolume = value.getMaxLoadVolume(); 
			}
		}
		return maxLoadVolume;
	}
	
	public static class Builder extends AbstractContainerBuilder<Builder> {
		
		protected int emptyWeight = -1;
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

		public Builder withEmptyWeight(int emptyWeight) {
			this.emptyWeight = emptyWeight;
			
			return this;
		}
		
		public int getMaxLoadWeight() {
			int maxLoadWeight = -1;
			
			for(Rotation rotation : rotations) {
				if(rotation.getMaxLoadWeight() > maxLoadWeight) {
					maxLoadWeight = rotation.getMaxLoadWeight(); 
				}
			}
			return maxLoadWeight;
		}
		
		public long getMaxLoadVolume() {
			long maxLoadVolume = -1;
			
			for(Rotation rotation : rotations) {
				if(rotation.getLoadVolume() > maxLoadVolume) {
					maxLoadVolume = rotation.getLoadVolume(); 
				}
			}
			return maxLoadVolume;
		}

		public DefaultContainer build() {
			if(rotations.isEmpty()) {
				throw new IllegalStateException("No rotations");
			}
			if(emptyWeight == -1) {
				throw new IllegalStateException("Expected empty weight");
			}
			
			Rotation rotation = rotations.get(0);
			long volume = rotation.getVolume();
			for(int i = 1; i < rotations.size(); i++) {
				if(rotations.get(i).getVolume() != volume) {
					throw new IllegalStateException();
				}
			}
			
			return new DefaultContainer(name, volume, emptyWeight, getStackValues(), stack);
		}
		
		protected ContainerStackValue[] getStackValues() {
			if(fixed) {
				FixedContainerStackValue[] stackValues = new FixedContainerStackValue[rotations.size()];
				
				int stackWeight = stack.getWeight();

				for (int i = 0; i < rotations.size(); i++) {
					Rotation rotation = rotations.get(i);

					StackConstraint constraint = rotation.stackConstraint != null ? rotation.stackConstraint : defaultConstraint;
					
					stackValues[i] = new FixedContainerStackValue(
							rotation.dx, rotation.dy, rotation.dz, 
							constraint , 
							stackWeight, emptyWeight,
							rotation.loadDx, rotation.loadDy, rotation.loadDz, 
							rotation.getMaxLoadWeight()
							);
				}
				return stackValues;
			} else {
				DefaultContainerStackValue[] stackValues = new DefaultContainerStackValue[rotations.size()];
				
				for (int i = 0; i < rotations.size(); i++) {
					Rotation rotation = rotations.get(i);

					StackConstraint constraint = rotation.stackConstraint != null ? rotation.stackConstraint : defaultConstraint;

					stackValues[i] = new DefaultContainerStackValue(
							rotation.dx, rotation.dy, rotation.dz, 
							constraint,
							rotation.loadDx, rotation.loadDy, rotation.loadDz, 
							rotation.getMaxLoadWeight()
							);
				}
				return stackValues;
			}
		}
			

		public long getVolume() {
			return rotations.get(0).getVolume();
		}
	}
	
	protected final int emptyWeight;
	/** i.e. best of the stack values */
	protected final long maxLoadVolume;
	/** i.e. best of the stack values */
	protected final int maxLoadWeight;

	protected final long volume;
	protected final long minArea;
	protected final long maxArea;
	
	public Container(String name, long volume, int emptyWeight, long maxLoadVolume, int maxLoadWeight, long minArea, long maxArea) {
		super(name);
		
		this.emptyWeight = emptyWeight;
		this.maxLoadVolume = maxLoadVolume;
		this.maxLoadWeight = maxLoadWeight;
		
		this.volume = volume;
		this.minArea = minArea;
		this.maxArea = maxArea;
	}
	
	@Override
	public int getWeight() {
		return emptyWeight + getStack().getWeight();
	}

	public long getMaxLoadVolume() {
		return maxLoadVolume;
	}
	
	public int getMaxLoadWeight() {
		return maxLoadWeight;
	}
	
	@Override
	public abstract ContainerStackValue[] getStackValues();

	public abstract Stack getStack();
	
	public int getEmptyWeight() {
		return emptyWeight;
	}
	
	public int getMaxWeight() {
		return emptyWeight + maxLoadWeight;
	}

	public abstract boolean canLoad(Stackable box);
	
	@Override
	public abstract Container clone();
	
	@Override
	public long getVolume() {
		return volume;
	}
	
	@Override
	public long getMinimumArea() {
		return minArea;
	}
	
	@Override
	public long getMaximumArea() {
		return maxArea;
	}
}
