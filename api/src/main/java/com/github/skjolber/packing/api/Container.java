package com.github.skjolber.packing.api;

public abstract class Container extends Stackable {

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
			
			return new DefaultContainer(this);
		}
	}
	
	protected final int emptyWeight;
	/** i.e. best of the stack values */
	protected final long maxLoadVolume;
	/** i.e. best of the stack values */
	protected final int maxLoadWeight;

	protected final long volume;
	
	public Container(String name, long volume, int emptyWeight, long maxLoadVolume, int maxLoadWeight) {
		super(name);
		
		this.emptyWeight = emptyWeight;
		this.maxLoadVolume = maxLoadVolume;
		this.maxLoadWeight = maxLoadWeight;
		
		this.volume = volume;
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
	
}
