package com.github.skjolber.packing.api;

import java.io.Serializable;

public class HorizontalSupport implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private final SupportPlacement3D negative;
	private final SupportPlacement3D positive;
	
	private final long area;

	public HorizontalSupport(SupportPlacement3D negative, SupportPlacement3D positive, long area) {
		super();
		this.negative = negative;
		this.positive = positive;
		this.area = area;
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

}
