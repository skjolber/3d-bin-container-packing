package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.api.packager.IntermediatePlacementResult;

public class ComparatorIntermediatePlacementResultBuilder extends AbstractComparatorIntermediatePlacementResultBuilder<IntermediatePlacementResult, ComparatorIntermediatePlacementResultBuilder> {

	@Override
	protected IntermediatePlacementResult createIntermediatePlacementResult(BoxItem boxItem, Point point, BoxStackValue stackValue) {
		return new IntermediatePlacementResult(boxItem, stackValue, point);
	}


}
