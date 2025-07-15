package com.github.skjolber.packing.packer.plain;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.packager.BoxItemControls;
import com.github.skjolber.packing.api.packager.FilteredBoxItemGroups;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;
import com.github.skjolber.packing.api.packager.IntermediatePlacementResult;
import com.github.skjolber.packing.api.packager.PointControls;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemGroupComparator;
import com.github.skjolber.packing.iterator.AnyOrderBoxItemGroupIterator;
import com.github.skjolber.packing.iterator.BoxItemGroupIterator;
import com.github.skjolber.packing.iterator.FixedOrderBoxItemGroupIterator;
import com.github.skjolber.packing.packer.AbstractDefaultPackager;
import com.github.skjolber.packing.packer.AbstractPackagerBuilder;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * Selects the box with the highest volume first, then places it into the point with the lowest volume.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class PlainPackager extends AbstractDefaultPackager {
	
	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractPackagerBuilder<PlainPackager, Builder> {

		protected Comparator<PlainIntermediatePlacementResult> intermediatePlacementResultComparator;
		protected IntermediatePackagerResultComparator intermediatePackagerResultComparator;
		protected Comparator<BoxItemGroup> boxItemGroupComparator;
		protected Comparator<BoxItem> boxItemComparator;

		public Builder withBoxItemGroupComparator(Comparator<BoxItemGroup> boxItemGroupComparator) {
			this.boxItemGroupComparator = boxItemGroupComparator;
			return this;
		}
		
		public Builder withBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
			this.boxItemComparator = boxItemComparator;
			return this;
		}
		
		public Builder withPackResultComparator(Comparator<PlainIntermediatePlacementResult> c) {
			this.intermediatePlacementResultComparator = c;
			return this;
		}

		public Builder withIntermediatePlacementResultComparator(Comparator<PlainIntermediatePlacementResult> intermediatePlacementResultComparator) {
			this.intermediatePlacementResultComparator = intermediatePlacementResultComparator;
			return this;
		}
		
		public PlainPackager build() {
			if(intermediatePlacementResultComparator == null) {
				intermediatePlacementResultComparator = new PlainPlacementResultComparator();
			}
			if(intermediatePackagerResultComparator == null) {
				intermediatePackagerResultComparator = new DefaultIntermediatePackagerResultComparator();
			}
			if(boxItemComparator == null) {
				boxItemComparator = VolumeThenWeightBoxItemComparator.getInstance();
			}
			if(boxItemGroupComparator == null) {
				boxItemGroupComparator = VolumeThenWeightBoxItemGroupComparator.getInstance();
			}
			return new PlainPackager(intermediatePackagerResultComparator, intermediatePlacementResultComparator, boxItemComparator, boxItemGroupComparator);
		}
	}

	protected PlainIntermediatePlacementResultBuilderFactory intermediatePlacementResultBuilderFactory = new PlainIntermediatePlacementResultBuilderFactory();
	
	protected Comparator<PlainIntermediatePlacementResult> intermediatePlacementResultComparator;
	protected Comparator<BoxItem> boxItemComparator;
	protected Comparator<BoxItemGroup> boxItemGroupComparator;

	public PlainPackager(IntermediatePackagerResultComparator comparator, Comparator<PlainIntermediatePlacementResult> intermediatePlacementResultComparator, Comparator<BoxItem> boxItemComparator, Comparator<BoxItemGroup> boxItemGroupComparator) {
		super(comparator);
		
		this.intermediatePlacementResultComparator = intermediatePlacementResultComparator;
		this.boxItemComparator = boxItemComparator;
		this.boxItemGroupComparator = boxItemGroupComparator;
	}

	protected BoxItemGroupIterator createBoxItemGroupIterator(FilteredBoxItemGroups filteredBoxItemGroups, Order itemGroupOrder, Container container, ExtremePoints extremePoints) {
		if(itemGroupOrder == Order.FIXED) {
			return new AnyOrderBoxItemGroupIterator(filteredBoxItemGroups, container, extremePoints, boxItemGroupComparator);
		}
		return new FixedOrderBoxItemGroupIterator(filteredBoxItemGroups, container, extremePoints);
	}

	public IntermediatePlacementResult findBestPoint(FilteredBoxItemGroups groups, FilteredBoxItems boxItems, PointControls pointControls, Container container, ExtremePoints extremePoints, Stack stack) {
		return intermediatePlacementResultBuilderFactory.createIntermediatePlacementResultBuilder()
			.withExtremePoints(extremePoints)
			.withBoxItems(boxItems)
			.withBoxItemGroups(groups)
			.withPointControls(pointControls)
			.withStack(stack)
			.withContainer(container)
			.withIntermediatePlacementResultComparator(intermediatePlacementResultComparator)
			.withBoxItemComparator(boxItemComparator)
			.build();
	}

}
