package com.github.skjolber.packing.packer.plain;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.ControlledContainerItem;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.ExtremePoints;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemGroupComparator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.iterator.AnyOrderBoxItemGroupIterator;
import com.github.skjolber.packing.iterator.BoxItemGroupIterator;
import com.github.skjolber.packing.iterator.FixedOrderBoxItemGroupIterator;
import com.github.skjolber.packing.packer.AbstractBoxItemAdapter;
import com.github.skjolber.packing.packer.AbstractBoxItemGroupAdapter;
import com.github.skjolber.packing.packer.AbstractControlPackager;
import com.github.skjolber.packing.packer.AbstractPackagerResultBuilder;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResult;
import com.github.skjolber.packing.packer.EmptyIntermediatePackagerResult;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * Selects the box with the highest volume first, then places it into the point with the lowest volume.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class PlainPackager extends AbstractControlPackager<PlainPlacement, IntermediatePackagerResult, PlainPackager.PlainResultBuilder> {
	
	public static Builder newBuilder() {
		return new Builder();
	}

	protected class PlainBoxItemAdapter extends AbstractBoxItemAdapter<IntermediatePackagerResult> {

		public PlainBoxItemAdapter(List<BoxItem> boxItems, BoxPriority priority,
				ContainerItemsCalculator packagerContainerItems,
				PackagerInterruptSupplier interrupt) {
			super(boxItems, priority, packagerContainerItems, interrupt);
		}

		@Override
		protected IntermediatePackagerResult pack(List<BoxItem> remainingBoxItems, ControlledContainerItem containerItem,
				PackagerInterruptSupplier interrupt, BoxPriority priority, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			return PlainPackager.this.pack(remainingBoxItems, containerItem, interrupt, priority, abortOnAnyBoxTooBig);
		}

	}
	
	protected class PlainBoxItemGroupAdapter extends AbstractBoxItemGroupAdapter<IntermediatePackagerResult> {

		public PlainBoxItemGroupAdapter(List<BoxItemGroup> boxItemGroups,
				BoxPriority priority,
				ContainerItemsCalculator packagerContainerItems, 
				PackagerInterruptSupplier interrupt) {
			super(boxItemGroups, packagerContainerItems, priority, interrupt);
		}

		@Override
		protected IntermediatePackagerResult packGroup(List<BoxItemGroup> remainingBoxItemGroups, BoxPriority priority,
				ControlledContainerItem containerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig) {
			return PlainPackager.this.packGroup(remainingBoxItemGroups, priority, containerItem, interrupt, abortOnAnyBoxTooBig);
		}

	}
	
	public class PlainResultBuilder extends AbstractPackagerResultBuilder<PlainResultBuilder> {

		@Override
		public PackagerResult build() {
			validate();
			
			if( (items == null || items.isEmpty()) && (itemGroups == null || itemGroups.isEmpty())) {
				throw new IllegalStateException();
			}
			long start = System.currentTimeMillis();

			PackagerInterruptSupplierBuilder booleanSupplierBuilder = PackagerInterruptSupplierBuilder.builder();
			if(deadline != -1L) {
				booleanSupplierBuilder.withDeadline(deadline);
			}
			if(interrupt != null) {
				booleanSupplierBuilder.withInterrupt(interrupt);
			}

			booleanSupplierBuilder.withScheduledThreadPoolExecutor(getScheduledThreadPoolExecutor());

			PackagerInterruptSupplier interrupt = booleanSupplierBuilder.build();
			try {
				PackagerAdapter<IntermediatePackagerResult> adapter;
				if(items != null && !items.isEmpty()) {
					adapter = new PlainBoxItemAdapter(items, priority, new ContainerItemsCalculator(containers), interrupt);
				} else {
					adapter = new PlainBoxItemGroupAdapter(itemGroups, priority, new ContainerItemsCalculator(containers), interrupt);
				}
				List<Container> packList = packAdapter(maxContainerCount, interrupt, adapter);
				
				long duration = System.currentTimeMillis() - start;
				return new PackagerResult(packList, duration, false);
			} catch (PackagerInterruptedException e) {
				long duration = System.currentTimeMillis() - start;
				return new PackagerResult(Collections.emptyList(), duration, true);
			} finally {
				interrupt.close();
			}
		}
	}

	public static class Builder {

		protected Comparator<PlainPlacement> placementComparator;
		protected Comparator<IntermediatePackagerResult> packagerResultComparator;
		protected Comparator<BoxItemGroup> boxItemGroupComparator;
		protected Comparator<BoxItem> boxItemComparator;
		protected PlacementControlsBuilderFactory<PlainPlacement, PlainPlacementControlsBuilder> placementControlsBuilderFactory;
		
		public Builder withBoxItemGroupComparator(Comparator<BoxItemGroup> comparator) {
			this.boxItemGroupComparator = comparator;
			return this;
		}
		
		public Builder withBoxItemComparator(Comparator<BoxItem> comparator) {
			this.boxItemComparator = comparator;
			return this;
		}
		
		public Builder withPlacementComparator(Comparator<PlainPlacement> c) {
			this.placementComparator = c;
			return this;
		}
		
		public Builder withPackagerResultComparator(Comparator<IntermediatePackagerResult> comparator) {
			this.packagerResultComparator = comparator;
			return this;
		}
		
		public Builder withPlacementControlsBuilderFactory(PlacementControlsBuilderFactory<PlainPlacement, PlainPlacementControlsBuilder> factory) {
			this.placementControlsBuilderFactory = factory;
			return this;
		}

		public PlainPackager build() {
			if(placementComparator == null) {
				placementComparator = new PlainPlacementComparator();
			}
			if(packagerResultComparator == null) {
				packagerResultComparator = new DefaultIntermediatePackagerResultComparator<>();
			}
			if(boxItemComparator == null) {
				boxItemComparator = VolumeThenWeightBoxItemComparator.getInstance();
			}
			if(boxItemGroupComparator == null) {
				boxItemGroupComparator = VolumeThenWeightBoxItemGroupComparator.getInstance();
			}
			if(placementControlsBuilderFactory == null) {
				placementControlsBuilderFactory = new PlainPlacementControlsBuilderFactory();
			}
			return new PlainPackager(packagerResultComparator, placementComparator, boxItemComparator, boxItemGroupComparator, placementControlsBuilderFactory);
		}
	}

	protected PlacementControlsBuilderFactory<PlainPlacement, PlainPlacementControlsBuilder> placementControlsBuilderFactory;
	protected Comparator<PlainPlacement> intermediatePlacementResultComparator;
	protected Comparator<BoxItem> boxItemComparator;
	protected Comparator<BoxItemGroup> boxItemGroupComparator;

	public PlainPackager(Comparator<IntermediatePackagerResult> comparator, Comparator<PlainPlacement> intermediatePlacementResultComparator, Comparator<BoxItem> boxItemComparator, Comparator<BoxItemGroup> boxItemGroupComparator, PlacementControlsBuilderFactory<PlainPlacement, PlainPlacementControlsBuilder> placementControlsBuilderFactory) {
		super(comparator);

		this.placementControlsBuilderFactory = placementControlsBuilderFactory;
		this.intermediatePlacementResultComparator = intermediatePlacementResultComparator;
		this.boxItemComparator = boxItemComparator;
		this.boxItemGroupComparator = boxItemGroupComparator;
	}

	protected BoxItemGroupIterator createBoxItemGroupIterator(BoxItemGroupSource filteredBoxItemGroups, BoxPriority priority, Container container, ExtremePoints extremePoints) {
		if(priority == BoxPriority.CRONOLOGICAL || priority == BoxPriority.CRONOLOGICAL_ALLOW_SKIPPING) {
			return new FixedOrderBoxItemGroupIterator(filteredBoxItemGroups, container, extremePoints);
		}
		return new AnyOrderBoxItemGroupIterator(filteredBoxItemGroups, container, extremePoints, boxItemGroupComparator);
	}
	
	@Override
	protected PlainPlacementControls createControls(BoxItemSource boxItems, int offset, int length,
			BoxPriority priority, PointControls pointControls, Container container, ExtremePoints extremePoints,
			Stack stack) {
		
		return placementControlsBuilderFactory.createPlacementControlsBuilder()
				.withExtremePoints(extremePoints)
				.withBoxItems(boxItems, offset, length)
				.withPointControls(pointControls)
				.withPriority(priority)
				.withStack(stack)
				.withContainer(container)
				.withPlacementComparator(intermediatePlacementResultComparator)
				.withBoxItemComparator(boxItemComparator)
				.build();
	}

	@Override
	public PlainResultBuilder newResultBuilder() {
		return new PlainResultBuilder();
	}

	@Override
	protected IntermediatePackagerResult createIntermediatePackagerResult(ContainerItem containerItem, Stack stack) {
		return new DefaultIntermediatePackagerResult(containerItem, stack);
	}

	@Override
	protected IntermediatePackagerResult createEmptyIntermediatePackagerResult() {
		return EmptyIntermediatePackagerResult.EMPTY;
	}

}
