package com.github.skjolber.packing.packer.plain;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemGroupComparator;
import com.github.skjolber.packing.comparator.placement.DefaultPlacementComparatorFactory;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;
import com.github.skjolber.packing.comparator.placement.PlacementComparatorFactory;
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
import com.github.skjolber.packing.packer.LoadAwarePlacementControlsBuilderFactory;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * Selects the box with the highest volume first, then places it into the point with the lowest volume.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class PlainPackager extends AbstractControlPackager<Placement, PlainPackager.PlainResultBuilder> {
	
	public static Builder newBuilder() {
		return new Builder();
	}

	protected class PlainBoxItemAdapter extends AbstractBoxItemAdapter {

		public PlainBoxItemAdapter(List<BoxItem> boxItems, Order order,
				ContainerItemsCalculator packagerContainerItems,
				PackagerInterruptSupplier interrupt) {
			super(boxItems, order, packagerContainerItems, interrupt);
		}

		@Override
		protected IntermediatePackagerResult pack(List<BoxItem> remainingBoxItems, ControlledContainerItem containerItem,
				PackagerInterruptSupplier interrupt, Order order, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			return PlainPackager.this.pack(remainingBoxItems, containerItem, interrupt, order, abortOnAnyBoxTooBig, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, maxLoadIdenticalBoxCount);
		}

		@Override
		protected IntermediatePackagerResult copy(ControlledContainerItem controlledContainerItem, IntermediatePackagerResult result, int index) {
			return createIntermediatePackagerResult(controlledContainerItem, result.getStack());
		}

	}
	
	protected class PlainBoxItemGroupAdapter extends AbstractBoxItemGroupAdapter {

		public PlainBoxItemGroupAdapter(List<BoxItemGroup> boxItemGroups,
				Order order,
				ContainerItemsCalculator packagerContainerItems, 
				PackagerInterruptSupplier interrupt) {
			super(boxItemGroups, packagerContainerItems, order, interrupt);
		}

		@Override
		protected IntermediatePackagerResult packGroup(List<BoxItemGroup> remainingBoxItemGroups, Order order,
				ControlledContainerItem containerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig) {
			return PlainPackager.this.packGroup(remainingBoxItemGroups, order, containerItem, interrupt, abortOnAnyBoxTooBig, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, maxLoadIdenticalBoxCount);
		}
		
		@Override
		protected IntermediatePackagerResult copy(ControlledContainerItem controlledContainerItem, IntermediatePackagerResult result, int index) {
			return createIntermediatePackagerResult(controlledContainerItem, result.getStack());
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
				PackagerAdapter adapter;
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

		// only applies if no placementControlsBuilderFactory is provided
		protected boolean requireFullSupport;
		protected boolean calculateSupport;
		
		protected Comparator<IntermediatePackagerResult> packagerResultComparator;
		protected Comparator<BoxItemGroup> boxItemGroupComparator;
		protected PlacementControlsBuilderFactory placementControlsBuilderFactory;
		
		public Builder withCalculateSupport(boolean calculateSupport) {
			this.calculateSupport = calculateSupport;
			return this;
		}
		
		public Builder withRequireFullSupport(boolean requireFullSupport) {
			this.requireFullSupport = requireFullSupport;
			return this;
		}
		
		public Builder withBoxItemGroupComparator(Comparator<BoxItemGroup> comparator) {
			this.boxItemGroupComparator = comparator;
			return this;
		}
		
		public Builder withPackagerResultComparator(Comparator<IntermediatePackagerResult> comparator) {
			this.packagerResultComparator = comparator;
			return this;
		}
		
		public Builder withPlacementControlsBuilderFactory(PlacementControlsBuilderFactory factory) {
			this.placementControlsBuilderFactory = factory;
			return this;
		}

		public Builder withPlacementControlsBuilderFactory(Consumer<PlacementControlsBuilderFactoryBuilder> consumer) {
			PlacementControlsBuilderFactoryBuilder b = new PlacementControlsBuilderFactoryBuilder();
			consumer.accept(b);
			
			boolean requireFullSupport = b.requireFullSupport;
			boolean calculateSupport = b.calculateSupport;
			Comparator<BoxItem> boxItemComparator = b.boxItemComparator;
			
			if(boxItemComparator == null) {
				boxItemComparator = VolumeThenWeightBoxItemComparator.getInstance();
			}
			PlacementComparatorFactory factory = b.comparatorFactory != null
					? b.comparatorFactory
					: DefaultPlacementComparatorFactory.newFactory()
							.higherVolumeIsBetter().higherWeightIsBetter()
							.lowerAreaIsBetter().lowerZIsBetter();
			placementControlsBuilderFactory = new LoadAwarePlacementControlsBuilderFactory(factory, boxItemComparator, calculateSupport, requireFullSupport);
			
			return this;
		}
		
		public static class PlacementControlsBuilderFactoryBuilder {

			private boolean requireFullSupport;
			private boolean calculateSupport;
			private Comparator<BoxItem> boxItemComparator;
			private PlacementComparatorFactory comparatorFactory;
			
			public PlacementControlsBuilderFactoryBuilder withCalculateSupport(boolean calculateSupport) {
				this.calculateSupport = calculateSupport;
				return this;
			}
			
			public PlacementControlsBuilderFactoryBuilder withRequireFullSupport(boolean require) {
				this.requireFullSupport = require;
				return this;
			}
			
			public PlacementControlsBuilderFactoryBuilder withBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
				this.boxItemComparator = boxItemComparator;
				return this;
			}
			
			/**
			 * Wraps a fixed {@link PlacementComparator} via {@link PlacementComparatorFactory#of}
			 * so it is used as-is for every packing run, ignoring any disabled attributes.
			 */
			public PlacementControlsBuilderFactoryBuilder withPlacementComparator(PlacementComparator placementComparator) {
				this.comparatorFactory = PlacementComparatorFactory.of(placementComparator);
				return this;
			}

			/**
			 * Configures a {@link DefaultPlacementComparatorFactory.Builder} via a consumer.
			 * The factory is used dynamically — per-run, only constraint dimensions that
			 * are active for that run are included. Position dimensions added via the
			 * consumer are always included.
			 */
			public PlacementControlsBuilderFactoryBuilder withPlacementComparatorFactory(Consumer<DefaultPlacementComparatorFactory.Builder> consumer) {
				DefaultPlacementComparatorFactory.Builder f = DefaultPlacementComparatorFactory.newFactory();
				consumer.accept(f);
				this.comparatorFactory = f;
				return this;
			}

			/** Sets a pre-configured {@link PlacementComparatorFactory} directly. */
			public PlacementControlsBuilderFactoryBuilder withPlacementComparatorFactory(PlacementComparatorFactory factory) {
				this.comparatorFactory = factory;
				return this;
			}
		}
		
		public PlainPackager build() {
			if(packagerResultComparator == null) {
				packagerResultComparator = new DefaultIntermediatePackagerResultComparator();
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
			if(boxItemGroupComparator == null) {
				boxItemGroupComparator = VolumeThenWeightBoxItemGroupComparator.getInstance();
			}
			return new PlainPackager(packagerResultComparator, boxItemGroupComparator, placementControlsBuilderFactory);
		}
		
	}

	protected PlacementControlsBuilderFactory placementControlsBuilderFactory;
	protected Comparator<BoxItemGroup> boxItemGroupComparator;

	public PlainPackager(Comparator<IntermediatePackagerResult> comparator, Comparator<BoxItemGroup> boxItemGroupComparator, PlacementControlsBuilderFactory placementControlsBuilderFactory) {
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
	protected PlacementControls createControls(BoxItemSource boxItems, Order order, PointControls pointControls,
			Container container, PointCalculator pointCalculator, Stack stack, boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount, boolean loadIdenticalBox) {
		
		return placementControlsBuilderFactory.createPlacementControlsBuilder()
				.withPointCalculator(pointCalculator)
				.withBoxItems(boxItems)
				.withPointControls(pointControls)
				.withOrder(order)
				.withStack(stack)
				.withContainer(container)
				.withMaxLoad(maxLoadWeight, maxLoadPressure, maxLoadBoxCount)
				.withLoadIdenticalBox(loadIdenticalBox)
				.build();
	}

	@Override
	public PlainResultBuilder newResultBuilder() {
		return new PlainResultBuilder();
	}

	@Override
	protected IntermediatePackagerResult createIntermediatePackagerResult(ControlledContainerItem containerItem, Stack stack) {
		return new DefaultIntermediatePackagerResult(containerItem, stack);
	}

	@Override
	protected IntermediatePackagerResult createEmptyIntermediatePackagerResult() {
		return EmptyIntermediatePackagerResult.EMPTY;
	}

}
