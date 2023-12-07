package com.github.skjolber.packing.api;

import java.io.Serializable;

public class VerticalSupport implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private final SupportPlacement3D negative;
	private final SupportPlacement3D positive;
	
	private final long area;
	
	private int weightLimit;

	public VerticalSupport(SupportPlacement3D negative, SupportPlacement3D positive, long area, int weightLimit) {
		super();
		this.negative = negative;
		this.positive = positive;
		this.area = area;
		this.weightLimit = weightLimit;
	}

	public SupportPlacement3D getNegative() {
		return negative;
	}

	public SupportPlacement3D getPositive() {
		return positive;
	}

	public long getArea() {
		return area;
	}

	public void decrementWeightLimit(int weightLimit) {
		this.weightLimit -= weightLimit;
	}
	
	public int getWeightLimit() {
		return weightLimit;
	}

}
