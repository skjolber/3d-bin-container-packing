package com.github.skjolber.packing.api;

public class Box extends Stackable {

	protected final int weight;
	protected final StackValue[] rotations;
	protected final long volume;

	public Box(String name, long volume, int weight, StackValue[] rotations) {
		super(name);
		this.volume = volume;
		this.weight = weight;
		this.rotations = rotations;
	}

	@Override
	public int getWeight() {
		return weight;
	}

	@Override
	public StackValue[] getStackValues() {
		return rotations;
	}

	public long getVolume() {
		return volume;
	}
}
