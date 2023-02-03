package com.github.skjolber.packing.jmh.ep;

import com.github.skjolber.packing.ep.points3d.DefaultPlacement3D;

public class ExtremePoint3DEntry {

	private final int index;
	private final DefaultPlacement3D placement;

	public ExtremePoint3DEntry(int index, DefaultPlacement3D placement) {
		super();
		this.index = index;
		this.placement = placement;
	}

	public int getIndex() {
		return index;
	}

	public DefaultPlacement3D getPlacement() {
		return placement;
	}
}
