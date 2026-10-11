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
import com.github.skjolber.packing.comparator.LargestAreaBoxItemGroupComparator;
import com.github.skjolber.packing.ep.points2d.DefaultPointCalculator2D;

/**
 * Fills each container level by level: the box with the largest ground area starts a level, then the remaining boxes are stacked within the level. Stacks in 2D, i.e. only places boxes along the floor of each level.
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
			IntermediatePackagerResultComparator intermediatePackagerResultComparator = this.intermediatePackagerResultComparator;
			if(intermediatePackagerResultComparator == null) {
				intermediatePackagerResultComparator = new DefaultIntermediatePackagerResultComparator();
			}
			BoxItemGroupComparator boxItemGroupComparator = this.boxItemGroupComparator;
			if(boxItemGroupComparator == null) {
				boxItemGroupComparator = new LargestAreaBoxItemGroupComparator();
			}
			FastLargestAreaFitFirstPackager packager = createPackager(intermediatePackagerResultComparator, boxItemGroupComparator,
					getPlacementControlsBuilderFactory(), getFirstPlacementControlsBuilderFactory());
			if(containerPackingStrategyFactory != null) {
				packager.setContainerPackingStrategyFactory(containerPackingStrategyFactory);
			}
			return packager;
		}

		/**
		 * @param placementControlsBuilderFactory the placement controls
		 * @param firstPlacementControlsBuilderFactory the placement controls of the first placement of a level
		 * @return a new packager
		 */
		protected FastLargestAreaFitFirstPackager createPackager(IntermediatePackagerResultComparator intermediatePackagerResultComparator, BoxItemGroupComparator boxItemGroupComparator,
				PlacementControlsBuilderFactory placementControlsBuilderFactory, PlacementControlsBuilderFactory firstPlacementControlsBuilderFactory) {
			return new FastLargestAreaFitFirstPackager(intermediatePackagerResultComparator, boxItemGroupComparator, placementControlsBuilderFactory, firstPlacementControlsBuilderFactory);
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
