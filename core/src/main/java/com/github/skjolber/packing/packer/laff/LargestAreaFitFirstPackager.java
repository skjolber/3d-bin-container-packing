package com.github.skjolber.packing.packer.laff;

import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.LargestAreaBoxItemComparator;
import com.github.skjolber.packing.comparator.LargestAreaBoxItemGroupComparator;
import com.github.skjolber.packing.comparator.LargestAreaPlacementComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemGroupComparator;
import com.github.skjolber.packing.comparator.VolumeWeightAreaPointIntermediatePlacementResultComparator;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.packer.ComparatorPlacementControlsBuilderFactory;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class LargestAreaFitFirstPackager extends AbstractLargestAreaFitFirstPackager {
	
	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractLargestAreaFitFirstPackagerBuilder<Placement, Builder> {

		public LargestAreaFitFirstPackager build() {
			if(intermediatePackagerResultComparator == null) {
				intermediatePackagerResultComparator = new DefaultIntermediatePackagerResultComparator<>();
			}
			if(boxItemGroupComparator == null) {
				boxItemGroupComparator = VolumeThenWeightBoxItemGroupComparator.getInstance();
			}
			if(firstBoxItemGroupComparator == null) {
				firstBoxItemGroupComparator = new LargestAreaBoxItemGroupComparator();
			}
			if(placementControlsBuilderFactory == null) {
				VolumeThenWeightBoxItemComparator boxItemComparator = new VolumeThenWeightBoxItemComparator();
				VolumeWeightAreaPointIntermediatePlacementResultComparator placementComparator = new VolumeWeightAreaPointIntermediatePlacementResultComparator();
				placementControlsBuilderFactory = new ComparatorPlacementControlsBuilderFactory(placementComparator, boxItemComparator);
			}
			if(firstPlacementControlsBuilderFactory == null) {
				LargestAreaBoxItemComparator firstBoxItemComparator = new LargestAreaBoxItemComparator();
				LargestAreaPlacementComparator firstPlacementComparator = new LargestAreaPlacementComparator();
				firstPlacementControlsBuilderFactory = new ComparatorPlacementControlsBuilderFactory(firstPlacementComparator, firstBoxItemComparator);
			}
			return new LargestAreaFitFirstPackager(intermediatePackagerResultComparator, boxItemGroupComparator, firstBoxItemGroupComparator, placementControlsBuilderFactory, firstPlacementControlsBuilderFactory);
		}
	}

	public LargestAreaFitFirstPackager(
			Comparator<IntermediatePackagerResult> comparator,
			Comparator<BoxItemGroup> boxItemGroupComparator,
			Comparator<BoxItemGroup> firstBoxItemGroupComparator, 
			PlacementControlsBuilderFactory<Placement> placementControlsBuilderFactory,
			PlacementControlsBuilderFactory<Placement> firstPlacementControlsBuilderFactory
			) {
		super(comparator,
				boxItemGroupComparator, 
				firstBoxItemGroupComparator, 
				placementControlsBuilderFactory,
				firstPlacementControlsBuilderFactory
				);
	}

	@Override
	protected PointCalculator createPointCalculator() {
		return new DefaultPointCalculator3D();
	}

}
