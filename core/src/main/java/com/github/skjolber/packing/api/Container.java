package com.github.skjolber.packing.api;

public abstract class Container extends Stackable {

	public static class Builder extends AbstractContainerBuilder<Builder> {
		
		protected Integer emptyWeight;
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
			if(rotations.isEmpty()) {
				throw new IllegalStateException("No rotations");
			}
			if(emptyWeight != null) {
				throw new IllegalStateException("No empty weight");
			}
			
			long maxLoadVolume = -1L;
			int maxLoadWeight = -1;
			long volume = -1L;
			
			DefaultContainer defaultContainer;
			if(fixed) {
				FixedContainerStackValue[] stackValues = new FixedContainerStackValue[rotations.size()];
				
				int stackWeight = stack.getWeight();

				for (int i = 0; i < rotations.size(); i++) {
					Rotation rotation = rotations.get(i);
					
					stackValues[i] = new FixedContainerStackValue(
							rotation.dx, rotation.dy, rotation.dz, 
							rotation.maxSupportedWeight, rotation.maxSupportedCount, 
							stackWeight,
							pressureReference,
							rotation.loadDx, rotation.loadDy, rotation.loadDz, 
							emptyWeight, rotation.maxLoadWeight
							);
					
					if(stackValues[i].getMaxLoadWeight() > maxLoadWeight) {
						maxLoadWeight = stackValues[i].getMaxLoadWeight(); 
					}
					
					if(stackValues[i].getVolume() > maxLoadVolume) {
						maxLoadVolume = stackValues[i].getVolume(); 
					}
					
					if(volume == -1L) {
						volume = stackValues[i].getVolume();
					} else if(volume != stackValues[i].getVolume()) {
						throw new IllegalStateException("Expected equal volume for all rotations");
					}
				}
				
				defaultContainer = new DefaultContainer(name, volume, emptyWeight, maxLoadVolume, maxLoadWeight, stackValues, stack);
			} else {
				DefaultContainerStackValue[] stackValues = new DefaultContainerStackValue[rotations.size()];
				
				for (int i = 0; i < rotations.size(); i++) {
					Rotation rotation = rotations.get(i);
					
					stackValues[i] = new DefaultContainerStackValue(
							rotation.dx, rotation.dy, rotation.dz, 
							rotation.maxSupportedWeight, rotation.maxSupportedCount, 
							pressureReference,
							rotation.loadDx, rotation.loadDy, rotation.loadDz, 
							emptyWeight, rotation.maxLoadWeight,
							stack
							);
					
					if(stackValues[i].getMaxLoadWeight() > maxLoadWeight) {
						maxLoadWeight = stackValues[i].getMaxLoadWeight(); 
					}
					
					if(stackValues[i].getVolume() > maxLoadVolume) {
						maxLoadVolume = stackValues[i].getVolume(); 
					}
					
					if(volume == -1L) {
						volume = stackValues[i].getVolume();
					} else if(volume != stackValues[i].getVolume()) {
						throw new IllegalStateException("Expected equal volume for all rotations");
					}
				}
				defaultContainer = new DefaultContainer(name, volume, emptyWeight, maxLoadVolume, maxLoadWeight, stackValues, stack);
			}

			return defaultContainer;
		}
	}
	
	
	protected final int emptyWeight;
	protected final long maxLoadVolume;
	protected final int maxLoadWeight;

	public Container(String name, int emptyWeight, long maxLoadVolume, int maxLoadWeight) {
		super(name);
		
		this.emptyWeight = emptyWeight;
		this.maxLoadVolume = maxLoadVolume;
		this.maxLoadWeight = maxLoadWeight;
	}
	
	@Override
	public int getWeight() {
		return emptyWeight + getStack().getWeight();
	}

	public long getMaxLoadVolume() {
		return maxLoadVolume;
	}
	
	public long getMaxLoadWeight() {
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
	
}
