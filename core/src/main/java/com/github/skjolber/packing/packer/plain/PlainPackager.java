package com.github.skjolber.packing.packer.plain;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.DefaultPackagerInterrupt;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.control.placement.PlacementComparator;
import com.github.skjolber.packing.api.packager.control.placement.PlacementComparatorFactory;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.packager.strategy.ContainerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerStrategyFactory;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemGroupComparator;
import com.github.skjolber.packing.comparator.placement.DefaultPlacementComparatorFactory;
import com.github.skjolber.packing.iterator.AnyOrderBoxItemGroupIterator;
import com.github.skjolber.packing.iterator.BoxItemGroupIterator;
import com.github.skjolber.packing.iterator.FixedOrderBoxItemGroupIterator;
import com.github.skjolber.packing.packer.AbstractBoxItemAdapter;
import com.github.skjolber.packing.packer.AbstractBoxItemGroupAdapter;
import com.github.skjolber.packing.packer.AbstractControlPackager;
import com.github.skjolber.packing.packer.AbstractPackagerResultBuilder;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResult;
import com.github.skjolber.packing.packer.EmptyIntermediatePackagerResult;
import com.github.skjolber.packing.packer.LoadAwarePlacementControlsBuilderFactory;

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
				List<ContainerItem> containers,
				int containerCount, PackagerInterruptSupplier interrupt) {
			super(boxItems, order, containers, containerCount, interrupt);
		}

		private PlainBoxItemAdapter(PlainBoxItemAdapter source) {
			super(source);
		}

		@Override
		public PackagerSession fork() {
			return new PlainBoxItemAdapter(this);
		}

		@Override
		protected PlainBoxItemAdapter fresh(List<ContainerItem> containers, int containerCount) {
			return new PlainBoxItemAdapter(copyBoxItems(initialBoxItems), order, containers, containerCount, interrupt);
		}

		@Override
		protected IntermediatePackagerResult pack(List<BoxItem> remainingBoxItems, ContainerItem containerItem,
				PackagerInterruptSupplier interrupt, Order order, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			return PlainPackager.this.pack(remainingBoxItems, containerItem, interrupt, order, abortOnAnyBoxTooBig, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, maxLoadIdenticalBoxCount);
		}

		@Override
		protected IntermediatePackagerResult copy(ContainerItem controlledContainerItem, IntermediatePackagerResult result, int index) {
			return createIntermediatePackagerResult(controlledContainerItem, result.getStack());
		}

	}
	
	protected class PlainBoxItemGroupAdapter extends AbstractBoxItemGroupAdapter {

		public PlainBoxItemGroupAdapter(List<BoxItemGroup> boxItemGroups,
				Order order,
				List<ContainerItem> containers,
				int containerCount, PackagerInterruptSupplier interrupt) {
			super(boxItemGroups, containers, containerCount, order, interrupt);
		}

		private PlainBoxItemGroupAdapter(PlainBoxItemGroupAdapter source) {
			super(source);
		}

		@Override
		public PackagerSession fork() {
			return new PlainBoxItemGroupAdapter(this);
		}

		@Override
		protected PlainBoxItemGroupAdapter fresh(List<ContainerItem> containers, int containerCount) {
			return new PlainBoxItemGroupAdapter(copyBoxItemGroups(initialBoxItemGroups), order, containers, containerCount, interrupt);
		}

		@Override
		protected IntermediatePackagerResult packGroup(List<BoxItemGroup> remainingBoxItemGroups, Order order,
				ContainerItem containerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig) {
			return PlainPackager.this.packGroup(remainingBoxItemGroups, order, containerItem, interrupt, abortOnAnyBoxTooBig, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, maxLoadIdenticalBoxCount);
		}
		
		@Override
		protected IntermediatePackagerResult copy(ContainerItem controlledContainerItem, IntermediatePackagerResult result, int index) {
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
				PackagerSession adapter;
				if(items != null && !items.isEmpty()) {
					adapter = new PlainBoxItemAdapter(items, order, containers, maxContainerCount, interrupt);
				} else {
					adapter = new PlainBoxItemGroupAdapter(itemGroups, order, containers, maxContainerCount, interrupt);
				}
				ContainerResult result = packAdapter(interrupt, adapter);
				
				long duration = System.currentTimeMillis() - start;
				if(result == null) {
					return new PackagerResult(Collections.emptyList(), duration, false, -1);
				}
				return new PackagerResult(result.getPackList(), duration, false, result.getCost());
			} catch (PackagerInterruptedException e) {
				long duration = System.currentTimeMillis() - start;
				return new PackagerResult(Collections.emptyList(), duration, true, -1);
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
		protected ContainerStrategyFactory containerStrategyFactory;
		
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
		
		/**
		 * Set the factory which selects the container strategy: which containers to use, and in which order.
		 * By default, cost-aware packing is used when the containers have costs, otherwise the first container
		 * (in preference order) which holds the boxes.
		 *
		 * @param factory container strategy factory
		 * @return this builder
		 */
		public Builder withContainerStrategyFactory(ContainerStrategyFactory factory) {
			this.containerStrategyFactory = Objects.requireNonNull(factory);
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
			PlainPackager packager = new PlainPackager(packagerResultComparator, boxItemGroupComparator, placementControlsBuilderFactory);
			if(containerStrategyFactory != null) {
				packager.setContainerStrategyFactory(containerStrategyFactory);
			}
			return packager;
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
	protected IntermediatePackagerResult createIntermediatePackagerResult(ContainerItem containerItem, Stack stack) {
		return new DefaultIntermediatePackagerResult(containerItem, stack);
	}

	@Override
	protected IntermediatePackagerResult createEmptyIntermediatePackagerResult() {
		return EmptyIntermediatePackagerResult.EMPTY;
	}

}
