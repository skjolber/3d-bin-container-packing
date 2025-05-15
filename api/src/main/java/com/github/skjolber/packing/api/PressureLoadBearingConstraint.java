package com.github.skjolber.packing.api;

public class PressureLoadBearingConstraint implements LoadBearingConstraint {

	
	private int maximumPressure;
	
	@Override
	public boolean canLoad(StackValue value, int x, int y, int z, int weight) {
		return weight / ;
	}

	@Override
	public void loaded(StackValue value, int x, int y, int z, int weight) {
		currentWeight += weight;
	}
	
}
