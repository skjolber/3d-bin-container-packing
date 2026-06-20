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
import com.github.skjolber.packing.comparator.LowerZDelegatePlacementComparator;
import com.github.skjolber.packing.comparator.SupportDelegateComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemGroupComparator;
import com.github.skjolber.packing.comparator.VolumeWeightAreaMinZPlacementComparator;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.LoadAwarePlacementControlsBuilderFactory;

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

	public static class Builder extends AbstractLargestAreaFitFirstPackagerBuilder<Builder> {

		public LargestAreaFitFirstPackager build() {
			if(intermediatePackagerResultComparator == null) {
				intermediatePackagerResultComparator = new DefaultIntermediatePackagerResultComparator();
			}
			if(boxItemGroupComparator == null) {
				boxItemGroupComparator = VolumeThenWeightBoxItemGroupComparator.getInstance();
			}
			if(firstBoxItemGroupComparator == null) {
				firstBoxItemGroupComparator = new LargestAreaBoxItemGroupComparator();
			}
			if(placementControlsBuilderFactory == null) {
				VolumeThenWeightBoxItemComparator boxItemComparator = new VolumeThenWeightBoxItemComparator();
				Comparator<Placement> placementComparator = new VolumeWeightAreaMinZPlacementComparator();
				
				if(!requireFullSupport && calculateSupport) {
					placementComparator = new SupportDelegateComparator(placementComparator);
				}
				
				placementControlsBuilderFactory = new LoadAwarePlacementControlsBuilderFactory(placementComparator, boxItemComparator, calculateSupport, requireFullSupport);
			}
			if(firstPlacementControlsBuilderFactory == null) {
				LargestAreaBoxItemComparator firstBoxItemComparator = new LargestAreaBoxItemComparator();
				Comparator<Placement> firstPlacementComparator = new LowerZDelegatePlacementComparator(new LargestAreaPlacementComparator());
				
				if(!requireFullSupport && calculateSupport) {
					firstPlacementComparator = new SupportDelegateComparator(firstPlacementComparator);
				}
				
				firstPlacementControlsBuilderFactory = new LoadAwarePlacementControlsBuilderFactory(firstPlacementComparator, firstBoxItemComparator, calculateSupport, requireFullSupport);
			}
			return new LargestAreaFitFirstPackager(intermediatePackagerResultComparator, boxItemGroupComparator, firstBoxItemGroupComparator, placementControlsBuilderFactory, firstPlacementControlsBuilderFactory);
		}
	}

	public LargestAreaFitFirstPackager(
			Comparator<IntermediatePackagerResult> comparator,
			Comparator<BoxItemGroup> boxItemGroupComparator,
			Comparator<BoxItemGroup> firstBoxItemGroupComparator, 
			PlacementControlsBuilderFactory placementControlsBuilderFactory,
			PlacementControlsBuilderFactory firstPlacementControlsBuilderFactory
			) {
		super(comparator,
				boxItemGroupComparator, 
				firstBoxItemGroupComparator, 
				placementControlsBuilderFactory,
				firstPlacementControlsBuilderFactory
				);
	}

	@Override
	protected PointCalculator createPointCalculator(BoxItemSource source) {
		return new DefaultPointCalculator3D(false, source);
	}

}
