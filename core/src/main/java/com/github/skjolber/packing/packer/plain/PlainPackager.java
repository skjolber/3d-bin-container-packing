package com.github.skjolber.packing.packer.plain;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
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
import com.github.skjolber.packing.packer.ControlledContainerItem;
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

		public PlainBoxItemAdapter(List<BoxItem> boxItems, Order order,
				ContainerItemsCalculator packagerContainerItems,
				PackagerInterruptSupplier interrupt) {
			super(boxItems, order, packagerContainerItems, interrupt);
		}

		@Override
		protected IntermediatePackagerResult pack(List<BoxItem> remainingBoxItems, ControlledContainerItem containerItem,
				PackagerInterruptSupplier interrupt, Order order, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			return PlainPackager.this.pack(remainingBoxItems, containerItem, interrupt, order, abortOnAnyBoxTooBig);
		}

	}
	
	protected class PlainBoxItemGroupAdapter extends AbstractBoxItemGroupAdapter<IntermediatePackagerResult> {

		public PlainBoxItemGroupAdapter(List<BoxItemGroup> boxItemGroups,
				Order order,
				ContainerItemsCalculator packagerContainerItems, 
				PackagerInterruptSupplier interrupt) {
			super(boxItemGroups, packagerContainerItems, order, interrupt);
		}

		@Override
		protected IntermediatePackagerResult packGroup(List<BoxItemGroup> remainingBoxItemGroups, Order order,
				ControlledContainerItem containerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig) {
			return PlainPackager.this.packGroup(remainingBoxItemGroups, order, containerItem, interrupt, abortOnAnyBoxTooBig);
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
					adapter = new PlainBoxItemAdapter(items, order, new ContainerItemsCalculator(containers), interrupt);
				} else {
					adapter = new PlainBoxItemGroupAdapter(itemGroups, order, new ContainerItemsCalculator(containers), interrupt);
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

		protected Comparator<IntermediatePackagerResult> packagerResultComparator;
		protected Comparator<BoxItemGroup> boxItemGroupComparator;
		protected PlacementControlsBuilderFactory<PlainPlacement> placementControlsBuilderFactory;
		
		public Builder withBoxItemGroupComparator(Comparator<BoxItemGroup> comparator) {
			this.boxItemGroupComparator = comparator;
			return this;
		}
		
		public Builder withPackagerResultComparator(Comparator<IntermediatePackagerResult> comparator) {
			this.packagerResultComparator = comparator;
			return this;
		}
		
		public Builder withPlacementControlsBuilderFactory(PlacementControlsBuilderFactory<PlainPlacement> factory) {
			this.placementControlsBuilderFactory = factory;
			return this;
		}

		public Builder withPlacementControlsBuilderFactory(Consumer<PlacementControlsBuilderFactoryBuilder> consumer) {
			PlacementControlsBuilderFactoryBuilder b = new PlacementControlsBuilderFactoryBuilder();
			consumer.accept(b);
			
			boolean requireFullSupport = b.requireFullSupport;
			Comparator<BoxItem> boxItemComparator = b.boxItemComparator;
			Comparator<PlainPlacement> placementComparator = b.placementComparator;
			
			if(boxItemComparator == null) {
				boxItemComparator = VolumeThenWeightBoxItemComparator.getInstance();
			}
			if(boxItemComparator == null) {
				placementComparator = new PlainPlacementComparator();
			}
			
			if(placementControlsBuilderFactory == null) {
				placementControlsBuilderFactory = new PlainPlacementControlsBuilderFactory(boxItemComparator, placementComparator, requireFullSupport);
			}
			
			return this;
		}
		
		public static class PlacementControlsBuilderFactoryBuilder {

			private boolean requireFullSupport;
			private Comparator<BoxItem> boxItemComparator;
			private Comparator<PlainPlacement> placementComparator; 
			
			public PlacementControlsBuilderFactoryBuilder withRequireFullSupport(boolean require) {
				this.requireFullSupport = require;
				return this;
			}
			
			public PlacementControlsBuilderFactoryBuilder withBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
				this.boxItemComparator = boxItemComparator;
				return this;
			}
			
			public PlacementControlsBuilderFactoryBuilder withPlacementComparator(Comparator<PlainPlacement> placementComparator) {
				this.placementComparator = placementComparator;
				return this;
			}

		}
		
		public PlainPackager build() {
			if(packagerResultComparator == null) {
				packagerResultComparator = new DefaultIntermediatePackagerResultComparator<>();
			}
			if(placementControlsBuilderFactory == null) {
				placementControlsBuilderFactory = new PlainPlacementControlsBuilderFactory();
			}
			if(boxItemGroupComparator == null) {
				boxItemGroupComparator = VolumeThenWeightBoxItemGroupComparator.getInstance();
			}
			return new PlainPackager(packagerResultComparator, boxItemGroupComparator, placementControlsBuilderFactory);
		}
		
	}

	protected PlacementControlsBuilderFactory<PlainPlacement> placementControlsBuilderFactory;
	protected Comparator<BoxItemGroup> boxItemGroupComparator;

	public PlainPackager(Comparator<IntermediatePackagerResult> comparator, Comparator<BoxItemGroup> boxItemGroupComparator, PlacementControlsBuilderFactory<PlainPlacement> placementControlsBuilderFactory) {
		super(comparator);

		this.placementControlsBuilderFactory = placementControlsBuilderFactory;
		this.boxItemGroupComparator = boxItemGroupComparator;
	}

	protected BoxItemGroupIterator createBoxItemGroupIterator(BoxItemGroupSource boxItemGroupSource, Order order, Container container, PointCalculator pointCalculator) {
		if(order == Order.CRONOLOGICAL || order == Order.CRONOLOGICAL_ALLOW_SKIPPING) {
			return new FixedOrderBoxItemGroupIterator(boxItemGroupSource, container, pointCalculator);
		}
		return new AnyOrderBoxItemGroupIterator(boxItemGroupSource, container, pointCalculator, boxItemGroupComparator);
	}
	
	@Override
	protected PlacementControls<PlainPlacement> createControls(BoxItemSource boxItems, int offset, int length,
			Order order, PointControls pointControls, Container container, PointCalculator pointCalculator,
			Stack stack) {
		
		return placementControlsBuilderFactory.createPlacementControlsBuilder()
				.withPointCalculator(pointCalculator)
				.withBoxItems(boxItems, offset, length)
				.withPointControls(pointControls)
				.withOrder(order)
				.withStack(stack)
				.withContainer(container)
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
