package com.github.skjolber.packing.jmh.ep;

import com.github.skjolber.packing.api.Placement;

public class Point3DEntry {

	private final int index;
	private final Placement placement;

	public Point3DEntry(int index, Placement placement) {
		super();
		this.index = index;
		this.placement = placement;
	}

	public int getIndex() {
		return index;
	}

	public Placement getPlacement() {
		return placement;
	}
}
