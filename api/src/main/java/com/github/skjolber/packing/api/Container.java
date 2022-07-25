package com.github.skjolber.packing.api;

public abstract class Container extends Stackable {

	private static final long serialVersionUID = 1L;

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
		
		public DefaultContainer build() {
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
			
			return new DefaultContainer(id, description, volume, emptyWeight, getStackValues(), stack);
		}
		
		protected ContainerStackValue[] getStackValues() {
			if(fixed) {
				FixedContainerStackValue[] stackValues = new FixedContainerStackValue[1];
				
				int stackWeight = stack.getWeight();
				
				stackValues[0] = new FixedContainerStackValue(
						dx, dy, dz, 
						stackConstraint, 
						stackWeight, emptyWeight,
						loadDx, loadDy, loadDz, 
						maxLoadWeight,
						surfaces
						);
				
				return stackValues;
			} else {
				DefaultContainerStackValue[] stackValues = new DefaultContainerStackValue[1];
				
				stackValues[0] = new DefaultContainerStackValue(
						dx, dy, dz, 
						stackConstraint,
						loadDx, loadDy, loadDz, 
						maxLoadWeight,
						surfaces
						);
				
				return stackValues;
			}
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
	
	public Container(String id, String name, long volume, int emptyWeight, long maxLoadVolume, int maxLoadWeight, long minArea, long maxArea) {
		super(id, name);
		
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
	
	public int getLoadWeight() {
		return getStack().getWeight();
	}
	
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

	public long getLoadVolume() {
		return getStack().getVolume();
	}
}
