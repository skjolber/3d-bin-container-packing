package com.github.skjolber.packing.jmh.ep;

import com.github.skjolber.packing.ep.points2d.DefaultPlacement2D;

public class ExtremePoint2DEntry {

	private final int index;
	private final DefaultPlacement2D placement;

	public ExtremePoint2DEntry(int index, DefaultPlacement2D placement) {
		super();
		this.index = index;
		this.placement = placement;
	}

	public int getIndex() {
		return index;
	}

	public DefaultPlacement2D getPlacement() {
		return placement;
	}
}
