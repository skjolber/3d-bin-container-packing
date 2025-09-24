package com.github.skjolber.packing.packer.laff;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.packager.IntermediatePlacement;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.point.ExtremePoints;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.LargestAreaBoxItemComparator;
import com.github.skjolber.packing.comparator.LargestAreaBoxItemGroupComparator;
import com.github.skjolber.packing.comparator.LargestAreaIntermediatePlacementResultComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemGroupComparator;
import com.github.skjolber.packing.comparator.VolumeWeightAreaPointIntermediatePlacementResultComparator;
import com.github.skjolber.packing.ep.points2d.ExtremePoints2D;
import com.github.skjolber.packing.packer.ComparatorIntermediatePlacementControlsBuilder;
import com.github.skjolber.packing.packer.ComparatorIntermediatePlacementControlsBuilderFactory;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. Only places boxes along the floor of each level.
 * <br>
 * <br>
 * Thread-safe implementation. The input boxes must however only be used in a single thread at a time.
 */

public class FastLargestAreaFitFirstPackager extends AbstractLargestAreaFitFirstPackager {
	
	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractLargestAreaFitFirstPackagerBuilder<Builder> {

		public FastLargestAreaFitFirstPackager build() {
			if(intermediatePlacementComparator == null) {
				intermediatePlacementComparator = new VolumeWeightAreaPointIntermediatePlacementResultComparator();
			}
			if(intermediatePackagerResultComparator == null) {
				intermediatePackagerResultComparator = new DefaultIntermediatePackagerResultComparator<>();
			}
			if(boxItemComparator == null) {
				boxItemComparator = VolumeThenWeightBoxItemComparator.getInstance();
			}
			if(boxItemGroupComparator == null) {
				boxItemGroupComparator = VolumeThenWeightBoxItemGroupComparator.getInstance();
			}
			if(firstBoxItemGroupComparator == null) {
				firstBoxItemGroupComparator = new LargestAreaBoxItemGroupComparator();
			}
			if(firstBoxItemComparator == null) {
				firstBoxItemComparator = new LargestAreaBoxItemComparator();
			}
			if(firstIntermediatePlacementComparator == null) {
				firstIntermediatePlacementComparator = new LargestAreaIntermediatePlacementResultComparator();
			}
			if(placementControlsBuilderFactory == null) {
				placementControlsBuilderFactory = new ComparatorIntermediatePlacementControlsBuilderFactory();
			}
			return new FastLargestAreaFitFirstPackager(intermediatePackagerResultComparator, intermediatePlacementComparator, boxItemComparator, boxItemGroupComparator, firstBoxItemGroupComparator, firstBoxItemComparator, firstIntermediatePlacementComparator, placementControlsBuilderFactory);
		}
	}

	public FastLargestAreaFitFirstPackager(Comparator<IntermediatePackagerResult> comparator,
			Comparator<IntermediatePlacement> intermediatePlacementResultComparator,
			Comparator<BoxItem> boxItemComparator, Comparator<BoxItemGroup> boxItemGroupComparator,
			Comparator<BoxItemGroup> firstBoxItemGroupComparator, Comparator<BoxItem> firstBoxItemComparator,
			Comparator<IntermediatePlacement> firstIntermediatePlacementResultComparator, PlacementControlsBuilderFactory<IntermediatePlacement, ComparatorIntermediatePlacementControlsBuilder> placementControlsBuilderFactory) {
		super(comparator, intermediatePlacementResultComparator, boxItemComparator, boxItemGroupComparator,
				firstBoxItemGroupComparator, firstBoxItemComparator, firstIntermediatePlacementResultComparator, placementControlsBuilderFactory);
	}

	@Override
	protected ExtremePoints createExtremePoints() {
		return new ExtremePoints2D();
	}
}
