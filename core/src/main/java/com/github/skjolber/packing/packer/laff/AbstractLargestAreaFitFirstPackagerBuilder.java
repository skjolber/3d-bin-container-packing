package com.github.skjolber.packing.packer.laff;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.packer.ComparatorPlacementControlsBuilder;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;

public abstract class AbstractLargestAreaFitFirstPackagerBuilder<B extends AbstractLargestAreaFitFirstPackagerBuilder<B>> {

	protected Comparator<Placement> placementComparator;
	protected Comparator<IntermediatePackagerResult> intermediatePackagerResultComparator;
	protected Comparator<BoxItemGroup> boxItemGroupComparator;
	protected Comparator<BoxItem> boxItemComparator;
	
	protected Comparator<Placement> firstPlacementComparator;
	protected Comparator<BoxItemGroup> firstBoxItemGroupComparator;
	protected Comparator<BoxItem> firstBoxItemComparator;

	protected PlacementControlsBuilderFactory<Placement, ComparatorPlacementControlsBuilder> placementControlsBuilderFactory;

	public B withFirstBoxItemGroupComparator(Comparator<BoxItemGroup> boxItemGroupComparator) {
		this.firstBoxItemGroupComparator = boxItemGroupComparator;
		return (B)this;
	}

	public B withFirstBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
		this.firstBoxItemComparator = boxItemComparator;
		return (B)this;
	}

	public B withBoxItemGroupComparator(Comparator<BoxItemGroup> boxItemGroupComparator) {
		this.boxItemGroupComparator = boxItemGroupComparator;
		return (B)this;
	}
	
	public B withBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
		this.boxItemComparator = boxItemComparator;
		return (B)this;
	}
	
	public B withFirstIntermediatePlacementResultComparator(Comparator<Placement> c) {
		this.firstPlacementComparator = c;
		return (B)this;
	}

	public B withIntermediatePlacementResultComparator(Comparator<Placement> comparator) {
		this.placementComparator = comparator;
		return (B)this;
	}
	
	public B withPlacementControlsBuilderFactory(
			PlacementControlsBuilderFactory<Placement, ComparatorPlacementControlsBuilder> placementControlsBuilderFactory) {
		this.placementControlsBuilderFactory = placementControlsBuilderFactory;
		return (B)this;
	}
	
}