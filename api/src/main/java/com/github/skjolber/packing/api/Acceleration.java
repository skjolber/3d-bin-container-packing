package com.github.skjolber.packing.api;

public class Acceleration {

	protected final int maxIncrease; // speed up
	protected final int maxDecrease; // slow down

	public Acceleration(int maxIncrease, int maxDecrease) {
		this.maxIncrease = maxIncrease;
		this.maxDecrease = maxDecrease;
	}

	public int getMaxDecrease() {
		return maxDecrease;
	}

	public int getMaxIncrease() {
		return maxIncrease;
	}
}
