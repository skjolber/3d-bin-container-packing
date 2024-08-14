package com.github.skjolber.packing.jmh.ep;

import com.github.skjolber.packing.api.StackPlacement;

public class ExtremePoint2DEntry {

	private final int index;
	private final StackPlacement placement;

	public ExtremePoint2DEntry(int index, StackPlacement placement) {
		super();
		this.index = index;
		this.placement = placement;
	}

	public int getIndex() {
		return index;
	}

	public StackPlacement getPlacement() {
		return placement;
	}
}
