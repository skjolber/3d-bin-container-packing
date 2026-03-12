package com.github.skjolber.packing.packer.laff;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.LargestAreaBoxItemComparator;
import com.github.skjolber.packing.comparator.LargestAreaBoxItemGroupComparator;
import com.github.skjolber.packing.comparator.LargestAreaPlacementComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.comparator.VolumeWeightAreaPointIntermediatePlacementResultComparator;
import com.github.skjolber.packing.ep.points2d.DefaultPointCalculator2D;
import com.github.skjolber.packing.packer.ComparatorPlacementControlsBuilderFactory;
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

	public static class Builder extends AbstractLargestAreaFitFirstPackagerBuilder<Placement, Builder> {

		public FastLargestAreaFitFirstPackager build() {
			if(intermediatePackagerResultComparator == null) {
				intermediatePackagerResultComparator = new DefaultIntermediatePackagerResultComparator();
			}
			if(boxItemGroupComparator == null) {
				boxItemGroupComparator = new LargestAreaBoxItemGroupComparator();
			}
			if(firstBoxItemGroupComparator == null) {
				firstBoxItemGroupComparator = new LargestAreaBoxItemGroupComparator();
			}
			if(firstPlacementControlsBuilderFactory == null) {
				LargestAreaBoxItemComparator firstBoxItemComparator = new LargestAreaBoxItemComparator();
				LargestAreaPlacementComparator firstPlacementComparator = new LargestAreaPlacementComparator();
				firstPlacementControlsBuilderFactory = new ComparatorPlacementControlsBuilderFactory(firstPlacementComparator, firstBoxItemComparator);
			}
			if(placementControlsBuilderFactory == null) {
				VolumeThenWeightBoxItemComparator boxItemComparator = new VolumeThenWeightBoxItemComparator();
				VolumeWeightAreaPointIntermediatePlacementResultComparator placementComparator = new VolumeWeightAreaPointIntermediatePlacementResultComparator();
				placementControlsBuilderFactory = new ComparatorPlacementControlsBuilderFactory(placementComparator, boxItemComparator);
			}
			return new FastLargestAreaFitFirstPackager(intermediatePackagerResultComparator, boxItemGroupComparator, firstBoxItemGroupComparator, placementControlsBuilderFactory, firstPlacementControlsBuilderFactory);
		}
	}

	public FastLargestAreaFitFirstPackager(
			Comparator<IntermediatePackagerResult> comparator,
			Comparator<BoxItemGroup> boxItemGroupComparator,
			Comparator<BoxItemGroup> firstBoxItemGroupComparator, 
			PlacementControlsBuilderFactory<Placement> placementControlsBuilderFactory,
			PlacementControlsBuilderFactory<Placement> firstPlacementControlsBuilderFactory) {
		super(comparator, 
				boxItemGroupComparator, 
				firstBoxItemGroupComparator, 
				placementControlsBuilderFactory,
				firstPlacementControlsBuilderFactory
				);
	}

	@Override
	protected PointCalculator createPointCalculator(BoxItemSource source) {
		return new DefaultPointCalculator2D(false, source);
	}
}
