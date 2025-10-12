package com.github.skjolber.packing.packer.laff;

import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;

public abstract class AbstractLargestAreaFitFirstPackagerBuilder<R extends Placement, B extends AbstractLargestAreaFitFirstPackagerBuilder<R, B>> {

	protected Comparator<IntermediatePackagerResult> intermediatePackagerResultComparator;
	
	protected Comparator<BoxItemGroup> firstBoxItemGroupComparator;
	protected Comparator<BoxItemGroup> boxItemGroupComparator;

	protected PlacementControlsBuilderFactory<R> firstPlacementControlsBuilderFactory;
	protected PlacementControlsBuilderFactory<R> placementControlsBuilderFactory;
	
	protected List<Point> points;

	public B withIntermediatePackagerResultComparator(Comparator<IntermediatePackagerResult> comparator) {
		this.intermediatePackagerResultComparator = comparator;
		return (B)this;
	}
	
	public B withPoints(List<Point> points) {
		this.points = points;
		return (B)this;
	}
	
	public B withFirstBoxItemGroupComparator(Comparator<BoxItemGroup> boxItemGroupComparator) {
		this.firstBoxItemGroupComparator = boxItemGroupComparator;
		return (B)this;
	}

	public B withBoxItemGroupComparator(Comparator<BoxItemGroup> boxItemGroupComparator) {
		this.boxItemGroupComparator = boxItemGroupComparator;
		return (B)this;
	}
	
	public B withPlacementControlsBuilderFactory(
			PlacementControlsBuilderFactory<R> placementControlsBuilderFactory) {
		this.placementControlsBuilderFactory = placementControlsBuilderFactory;
		return (B)this;
	}
	
	public B withFirstPlacementControlsBuilderFactory(
			PlacementControlsBuilderFactory<R> placementControlsBuilderFactory) {
		this.firstPlacementControlsBuilderFactory = placementControlsBuilderFactory;
		return (B)this;
	}
	
}