package com.github.skjolber.packing.api;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public abstract class Stack implements Serializable {

	private static final long serialVersionUID = 1L;

	protected ContainerStackValue containerStackValue;

	public Stack() {
	}

	public Stack(ContainerStackValue containerStackValue) {
		super();
		this.containerStackValue = containerStackValue;
	}

	public ContainerStackValue getContainerStackValue() {
		return containerStackValue;
	}

	public abstract List<StackPlacement> getPlacements();

	public abstract void add(StackPlacement e);

	public abstract void remove(StackPlacement e);

	public BigDecimal getFreeVolumeLoad() {
		return containerStackValue.getMaxLoadVolume().subtract(getVolume());
	}

	public BigDecimal getFreeWeightLoad() {
		return containerStackValue.getMaxLoadWeight().subtract(getWeight());
	}

	public abstract BigDecimal getWeight();

	public abstract BigDecimal getDz();

	public abstract BigDecimal getVolume();

	public abstract void clear();

	public abstract boolean isEmpty();

	public void addAll(List<StackPlacement> placements) {
		for (StackPlacement p : placements) {
			add(p);
		}
	}

	public abstract int getSize();

	public abstract void setSize(int size);

}
