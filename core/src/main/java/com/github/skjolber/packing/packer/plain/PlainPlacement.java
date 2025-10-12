package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;

public class PlainPlacement extends Placement {

	private static final long serialVersionUID = 1L;
	
	protected long supportedArea = -1L;

	public PlainPlacement(BoxStackValue stackValue, Point point, long supportedArea) {
		super(stackValue, point);
		
		this.supportedArea = supportedArea;
	}

	public long getSupportArea() {
		return supportedArea;
	}

}