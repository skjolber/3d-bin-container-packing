package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.packager.IntermediatePlacement;
import com.github.skjolber.packing.api.point.Point;

public class PlainPlacement extends IntermediatePlacement {

	protected long bestPointSupportPercent = -1;

	public PlainPlacement(int index, BoxStackValue stackValue, Point point, long bestPointSupportPercent) {
		super(index, stackValue, point);
		
		this.bestPointSupportPercent = bestPointSupportPercent;
	}

	public long getBestPointSupportPercent() {
		return bestPointSupportPercent;
	}

}