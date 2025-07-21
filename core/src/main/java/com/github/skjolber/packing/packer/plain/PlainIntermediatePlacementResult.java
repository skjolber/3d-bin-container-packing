package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.api.packager.IntermediatePlacementResult;

public class PlainIntermediatePlacementResult extends IntermediatePlacementResult {

	protected long bestPointSupportPercent = -1;

	public PlainIntermediatePlacementResult(int index, BoxStackValue stackValue, Point point, long bestPointSupportPercent) {
		super(index, stackValue, point);
		
		this.bestPointSupportPercent = bestPointSupportPercent;
	}

	public long getBestPointSupportPercent() {
		return bestPointSupportPercent;
	}

}