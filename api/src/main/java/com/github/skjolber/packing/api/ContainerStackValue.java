package com.github.skjolber.packing.api;

import java.math.BigDecimal;
import java.util.List;

public abstract class ContainerStackValue extends StackValue {

	private static final long serialVersionUID = 1L;

	public ContainerStackValue(
			BigDecimal dx, BigDecimal dy, BigDecimal dz,
			StackConstraint constraint,
			BigDecimal loadDx, BigDecimal loadDy, BigDecimal loadDz, BigDecimal maxLoadWeight, List<Surface> surfaces) {
		super(dx, dy, dz, constraint, surfaces);

		this.loadDx = loadDx;
		this.loadDy = loadDy;
		this.loadDz = loadDz;

		this.maxLoadVolume = loadDx.multiply(loadDy).multiply(loadDz);
		this.maxLoadWeight = maxLoadWeight;
	}

	protected final BigDecimal loadDx; // x
	protected final BigDecimal loadDy; // y
	protected final BigDecimal loadDz; // z

	protected final BigDecimal maxLoadWeight;
	protected final BigDecimal maxLoadVolume;

	public BigDecimal getMaxLoadVolume() {
		return maxLoadVolume;
	}

	public BigDecimal getMaxLoadWeight() {
		return maxLoadWeight;
	}

	public BigDecimal getLoadDx() {
		return loadDx;
	}

	public BigDecimal getLoadDy() {
		return loadDy;
	}

	public BigDecimal getLoadDz() {
		return loadDz;
	}

	protected boolean canLoad(Stackable stackable) {
		if(stackable.getWeight().compareTo(maxLoadWeight) > 0) {
			return false;
		}
		for (StackValue stackValue : stackable.getStackValues()) {
			if(
				stackValue.getDx().compareTo(loadDx) <= 0 &&
						stackValue.getDy().compareTo(loadDy) <= 0 &&
						stackValue.getDz().compareTo(loadDz) <= 0
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

}
