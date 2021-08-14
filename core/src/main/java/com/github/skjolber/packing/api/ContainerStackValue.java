package com.github.skjolber.packing.api;

public abstract class ContainerStackValue extends StackValue {

	public ContainerStackValue(
			int dx, int dy, int dz, 
			StackConstraint constraint,
			int loadDx, int loadDy, int loadDz, int maxLoadWeight, Container container) {
		super(dx, dy, dz, constraint);
		
		this.loadDx = loadDx;
		this.loadDy = loadDy;
		this.loadDz = loadDz;
		
		this.container = container;
		
		this.loadVolume = (long)loadDx * (long)loadDy * (long)loadDz;
		this.maxLoadWeight = maxLoadWeight;
	}
	
	protected final Container container;
	
	protected final int loadDx; // x
	protected final int loadDy; // y
	protected final int loadDz; // z
	
	protected final int maxLoadWeight;
	protected final long loadVolume;
	
	public long getMaxLoadVolume() {
		return loadVolume;
	}
	
	public int getMaxLoadWeight() {
		return maxLoadWeight;
	}

	public int getLoadDx() {
		return loadDx;
	}
	
	public int getLoadDy() {
		return loadDy;
	}
	
	public int getLoadDz() {
		return loadDz;
	}

	protected boolean canLoad(Stackable stackable) {
		for (StackValue stackValue : stackable.getStackValues()) {
			if(
				stackValue.getDx() <= loadDx && 
				stackValue.getDy() <= loadDy && 
				stackValue.getDz() <= loadDz &&
				stackValue.getWeight() <= maxLoadWeight
				) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public Container getStackable() {
		return container;
	}

}
