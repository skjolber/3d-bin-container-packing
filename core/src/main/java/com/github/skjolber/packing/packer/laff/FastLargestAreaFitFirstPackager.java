package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.BoxItemGroupComparator;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.LargestAreaBoxItemComparator;
import com.github.skjolber.packing.comparator.LargestAreaBoxItemGroupComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.comparator.placement.DefaultPlacementComparatorFactory;
import com.github.skjolber.packing.ep.points2d.DefaultPointCalculator2D;
import com.github.skjolber.packing.packer.LoadAwarePlacementControlsBuilderFactory;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. Only places boxes along the floor of each level.
 * <br>
 * <br>
 * Thread-safe implementation. Packing works on copies of the input boxes and containers; it only assigns global indexes
 * to box items which have none (see {@code BoxItem.getGlobalIndex()}), so assign them before packing the same box items concurrently.
 */

public class FastLargestAreaFitFirstPackager extends AbstractLargestAreaFitFirstPackager {
	
	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractLargestAreaFitFirstPackagerBuilder<Builder> {

		public FastLargestAreaFitFirstPackager build() {
			checkSupportOptions();
			if(intermediatePackagerResultComparator == null) {
				intermediatePackagerResultComparator = new DefaultIntermediatePackagerResultComparator();
			}
			if(boxItemGroupComparator == null) {
				boxItemGroupComparator = new LargestAreaBoxItemGroupComparator();
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
				firstPlacementControlsBuilderFactory = new LoadAwarePlacementControlsBuilderFactory(firstFactory.compile(), firstBoxItemComparator, calculateSupport, requireFullSupport);
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
				placementControlsBuilderFactory = new LoadAwarePlacementControlsBuilderFactory(placementFactory.compile(), boxItemComparator, calculateSupport, requireFullSupport);
			}
			FastLargestAreaFitFirstPackager packager = new FastLargestAreaFitFirstPackager(intermediatePackagerResultComparator, boxItemGroupComparator, placementControlsBuilderFactory, firstPlacementControlsBuilderFactory);
			if(containerStrategyFactory != null) {
				packager.setContainerStrategyFactory(containerStrategyFactory);
			}
			return packager;
		}
	}

	public FastLargestAreaFitFirstPackager(
			IntermediatePackagerResultComparator comparator,
			BoxItemGroupComparator boxItemGroupComparator,
			PlacementControlsBuilderFactory placementControlsBuilderFactory,
			PlacementControlsBuilderFactory firstPlacementControlsBuilderFactory) {
		super(comparator, 
				boxItemGroupComparator, 
				placementControlsBuilderFactory,
				firstPlacementControlsBuilderFactory
				);
	}

	@Override
	protected PointCalculator createPointCalculator(BoxItemSource source) {
		return new DefaultPointCalculator2D(false, source);
	}
}
