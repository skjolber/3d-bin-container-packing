package com.github.skjolber.packing.api;

public class WeightLoadBearingConstraint implements LoadBearingConstraint {

	private int maximumWeight;
	
	private int currentWeight;

	@Override
	public boolean canLoad(StackValue value, int x, int y, int z, int weight) {
		return currentWeight + weight <= maximumWeight;
	}

	@Override
	public void loaded(StackValue value, int x, int y, int z, int weight) {
		currentWeight += weight;
	}
	
}
