package com.github.skjolber.packing.packer.laff;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.packager.IntermediatePlacement;
import com.github.skjolber.packing.api.packager.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.packer.ComparatorIntermediatePlacementControlsBuilder;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;

public abstract class AbstractLargestAreaFitFirstPackagerBuilder<B extends AbstractLargestAreaFitFirstPackagerBuilder<B>> {

	protected Comparator<IntermediatePlacement> intermediatePlacementComparator;
	protected Comparator<IntermediatePackagerResult> intermediatePackagerResultComparator;
	protected Comparator<BoxItemGroup> boxItemGroupComparator;
	protected Comparator<BoxItem> boxItemComparator;
	
	protected Comparator<IntermediatePlacement> firstIntermediatePlacementComparator;
	protected Comparator<BoxItemGroup> firstBoxItemGroupComparator;
	protected Comparator<BoxItem> firstBoxItemComparator;

	protected PlacementControlsBuilderFactory<IntermediatePlacement, ComparatorIntermediatePlacementControlsBuilder> placementControlsBuilderFactory;

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
	
	public B withFirstIntermediatePlacementResultComparator(Comparator<IntermediatePlacement> c) {
		this.firstIntermediatePlacementComparator = c;
		return (B)this;
	}

	public B withIntermediatePlacementResultComparator(
			Comparator<IntermediatePlacement> intermediatePlacementResultComparator) {
		this.intermediatePlacementComparator = intermediatePlacementResultComparator;
		return (B)this;
	}
	
	public B withPlacementControlsBuilderFactory(
			PlacementControlsBuilderFactory<IntermediatePlacement, ComparatorIntermediatePlacementControlsBuilder> placementControlsBuilderFactory) {
		this.placementControlsBuilderFactory = placementControlsBuilderFactory;
		return (B)this;
	}
	
}