package com.github.skjolber.packing.api;

import java.util.List;

public abstract class ContainerStackValue extends StackValue {

	private static final long serialVersionUID = 1L;

	private Container stackable;

	protected final int loadDx; // x
	protected final int loadDy; // y
	protected final int loadDz; // z

	protected final int maxLoadWeight;
	protected final long maxLoadVolume;

	public ContainerStackValue(
			int dx, int dy, int dz,
			StackValueConstraint constraint,
			int loadDx, int loadDy, int loadDz, int maxLoadWeight, List<Surface> surfaces) {
		super(dx, dy, dz, constraint, surfaces);

		this.loadDx = loadDx;
		this.loadDy = loadDy;
		this.loadDz = loadDz;

		this.maxLoadVolume = (long)loadDx * (long)loadDy * (long)loadDz;
		this.maxLoadWeight = maxLoadWeight;
	}
	
	protected ContainerStackValue(ContainerStackValue other) {
		super(other);
		
		this.loadDx = other.loadDx;
		this.loadDy = other.loadDy;
		this.loadDz = other.loadDz;

		this.maxLoadVolume = other.maxLoadVolume;
		this.maxLoadWeight = other.maxLoadWeight;
		
		this.stackable = other.stackable;
	}
	
	public long getMaxLoadVolume() {
		return maxLoadVolume;
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
		if(stackable.getWeight() > maxLoadWeight) {
			return false;
		}
		for (StackValue stackValue : stackable.getStackValues()) {
			if(
				stackValue.getDx() <= loadDx &&
						stackValue.getDy() <= loadDy &&
						stackValue.getDz() <= loadDz
			) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "ContainerStackValue [" + dx + "x" + dy + "x" + dz + " " + loadDx + "x" + loadDy + "x" + loadDz + "]";
	}
	
	public void setStackable(Container stackable) {
		this.stackable = stackable;
	}
	
	@Override
	public Container getStackable() {
		return stackable;
	}
	
	@Override
	public abstract ContainerStackValue clone();

}
