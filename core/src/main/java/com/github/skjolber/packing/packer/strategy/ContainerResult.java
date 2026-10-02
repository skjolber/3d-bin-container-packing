package com.github.skjolber.packing.packer.strategy;

import java.util.List;

import com.github.skjolber.packing.api.Container;

public class ContainerResult {

	protected final long cost;
	protected final List<Container> packList;

	public ContainerResult(long cost, List<Container> packList) {
		this.cost = cost;
		this.packList = List.copyOf(packList);
	}

	public long getCost() {
		return cost;
	}

	public List<Container> getPackList() {
		return packList;
	}

}
