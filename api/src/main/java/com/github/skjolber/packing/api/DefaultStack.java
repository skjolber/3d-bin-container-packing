package com.github.skjolber.packing.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DefaultStack extends Stack {

	private static final long serialVersionUID = 1L;
	protected final List<StackPlacement> entries = new ArrayList<>();

	public DefaultStack() {
	}

	public DefaultStack(ContainerStackValue containerStackValue) {
		super(containerStackValue);
	}

	public List<StackPlacement> getPlacements() {
		return entries;
	}

	public void add(StackPlacement e) {
		entries.add(e);
	}

	@Override
	public void remove(StackPlacement e) {
		entries.remove(e);
	}

	public void clear() {
		entries.clear();
	}

	@Override
	public BigDecimal getWeight() {
		BigDecimal weight = BigDecimal.ZERO;

		for (StackPlacement stackEntry : entries) {
			weight = weight.add(stackEntry.getStackable().getWeight());
		}

		return weight;
	}

	@Override
	public BigDecimal getDz() {
		BigDecimal dz = BigDecimal.ZERO;

		for (StackPlacement stackEntry : entries) {
			dz = dz.max(stackEntry.getAbsoluteEndZ());
		}

		return dz;
	}

	@Override
	public BigDecimal getVolume() {
		BigDecimal volume = BigDecimal.ZERO;

		for (StackPlacement stackEntry : entries) {
			volume = volume.add(stackEntry.getStackable().getVolume());
		}

		return volume;
	}

	@Override
	public boolean isEmpty() {
		return entries.isEmpty();
	}

	@Override
	public int getSize() {
		return entries.size();
	}

	@Override
	public void setSize(int size) {
		while (size < entries.size()) {
			entries.remove(entries.size() - 1);
		}
	}

}
