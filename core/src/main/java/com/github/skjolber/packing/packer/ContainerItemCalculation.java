package com.github.skjolber.packing.packer;

import java.util.List;

public class ContainerItemCalculation {

	// by cost or by natural order
	private final List<Integer> order;
	
	private final List<Integer> volumeOrder;

	private final List<Integer> weightOrder;

	public ContainerItemCalculation(List<Integer> order, List<Integer> volumeOrder, List<Integer> weightOrder) {
		super();
		this.order = order;
		this.volumeOrder = volumeOrder;
		this.weightOrder = weightOrder;
	}
	
	public List<Integer> getOrder() {
		return order;
	}
	
	public List<Integer> getVolumeOrder() {
		return volumeOrder;
	}
	
	public List<Integer> getWeightOrder() {
		return weightOrder;
	}
	
}
