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
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.comparator.placement.DefaultPlacementComparatorFactory;
import com.github.skjolber.packing.ep.points2d.DefaultPointCalculator2D;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.LoadAwarePlacementControlsBuilderFactory;

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
				DefaultPlacementComparatorFactory.Builder firstFactory = DefaultPlacementComparatorFactory.newFactory();
				if(!requireFullSupport && calculateSupport) {
					firstFactory.higherSupportIsBetter();
				}
				firstFactory.lowerZIsBetter()
						.higherAreaIsBetter()
						.higherVolumeIsBetter()
						.higherWeightIsBetter();
				firstPlacementControlsBuilderFactory = new LoadAwarePlacementControlsBuilderFactory(firstFactory, firstBoxItemComparator, calculateSupport, requireFullSupport);
			}
			if(placementControlsBuilderFactory == null) {
				VolumeThenWeightBoxItemComparator boxItemComparator = new VolumeThenWeightBoxItemComparator();
				DefaultPlacementComparatorFactory.Builder placementFactory = DefaultPlacementComparatorFactory.newFactory();
				if(!requireFullSupport && calculateSupport) {
					placementFactory.higherSupportIsBetter();
				}
				placementFactory.higherVolumeIsBetter()
						.higherWeightIsBetter()
						.lowerAreaIsBetter()
						.lowerZIsBetter();
				placementControlsBuilderFactory = new LoadAwarePlacementControlsBuilderFactory(placementFactory, boxItemComparator, calculateSupport, requireFullSupport);
			}
			return new FastLargestAreaFitFirstPackager(intermediatePackagerResultComparator, boxItemGroupComparator, firstBoxItemGroupComparator, placementControlsBuilderFactory, firstPlacementControlsBuilderFactory);
		}
	}

	public FastLargestAreaFitFirstPackager(
			Comparator<IntermediatePackagerResult> comparator,
			Comparator<BoxItemGroup> boxItemGroupComparator,
			Comparator<BoxItemGroup> firstBoxItemGroupComparator, 
			PlacementControlsBuilderFactory placementControlsBuilderFactory,
			PlacementControlsBuilderFactory firstPlacementControlsBuilderFactory) {
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
