package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.IntermediatePlacement;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.ExtremePoints;
import com.github.skjolber.packing.api.point.Point;

public class ComparatorIntermediatePlacementControls extends AbstractComparatorPlacementControls<IntermediatePlacement>{

	public ComparatorIntermediatePlacementControls(BoxItemSource boxItems, int boxItemsStartIndex, int boxItemsEndIndex,
			PointControls pointControls, ExtremePoints extremePoints, Container container, Stack stack,
			BoxPriority priority, Comparator<IntermediatePlacement> intermediatePlacementResultComparator,
			Comparator<BoxItem> boxItemComparator) {
		super(boxItems, boxItemsStartIndex, boxItemsEndIndex, pointControls, extremePoints, container, stack, priority,
				intermediatePlacementResultComparator, boxItemComparator);
	}

	@Override
	protected IntermediatePlacement createIntermediatePlacementResult(int index, Point point3d, BoxStackValue stackValue) {
		return new IntermediatePlacement(index, stackValue, point3d);
	}

}
