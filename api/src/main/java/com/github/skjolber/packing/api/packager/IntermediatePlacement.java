package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.ep.Point;

public class IntermediatePlacement extends Placement {

	private static final long serialVersionUID = 1L;

	protected int index;

	public IntermediatePlacement(int index, BoxStackValue stackValue, Point point) {
		super();
		this.index = index;
		this.stackValue = stackValue;
		this.point = point;
	}

	public int getIndex() {
		return index;
	}

}