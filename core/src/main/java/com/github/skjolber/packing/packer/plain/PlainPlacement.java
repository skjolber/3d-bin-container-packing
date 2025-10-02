package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;

public class PlainPlacement extends Placement {

	private static final long serialVersionUID = 1L;
	
	protected long bestPointSupportPercent = -1;

	public PlainPlacement(BoxStackValue stackValue, Point point, long bestPointSupportPercent) {
		super(stackValue, point);
		
		this.bestPointSupportPercent = bestPointSupportPercent;
	}

	public long getBestPointSupportPercent() {
		return bestPointSupportPercent;
	}

}