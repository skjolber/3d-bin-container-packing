package com.github.skjolber.packing.packer.plain;

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
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.BoxItemGroupComparator;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.packager.strategy.ContainerPackingStrategyFactory;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemGroupComparator;
import com.github.skjolber.packing.iterator.AnyOrderBoxItemGroupIterator;
import com.github.skjolber.packing.iterator.BoxItemGroupIterator;
import com.github.skjolber.packing.iterator.FixedOrderBoxItemGroupIterator;
import com.github.skjolber.packing.packer.AbstractBoxItemGroupSession;
import com.github.skjolber.packing.packer.AbstractBoxItemSession;
import com.github.skjolber.packing.packer.AbstractControlPackager;
import com.github.skjolber.packing.packer.AbstractPackagerResultBuilder;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResult;
import com.github.skjolber.packing.packer.EmptyIntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerInput;
import com.github.skjolber.packing.packer.PlacementControlsBuilderFactoryBuilder;

/**
 * Packs each container by repeatedly selecting the box with the highest volume, then placing it into the point with the lowest volume.
 * <br>
 * <br>
 * Thread-safe implementation. Packing works on copies of the input boxes and containers; it only assigns global indexes
 * to box items which have none (see {@code BoxItem.getGlobalIndex()}), so assign them before packing the same box items concurrently.
 */

public class PlainPackager extends AbstractControlPackager<Placement, PlainPackager.PlainResultBuilder> {
	
	public static Builder newBuilder() {
		return new Builder();
	}

	protected class PlainBoxItemSession extends AbstractBoxItemSession {

		public PlainBoxItemSession(List<BoxItem> boxItems, Order order,
				List<ContainerItem> containers,
				int containerCount, PackagerInterruptSupplier interrupt) {
			super(boxItems, order, containers, containerCount, interrupt);
		}

		private PlainBoxItemSession(PlainBoxItemSession source) {
			super(source);
		}

		@Override
		public PackagerSession fork() {
			return new PlainBoxItemSession(this);
		}

		@Override
		protected PlainBoxItemSession fresh(List<ContainerItem> containers, int containerCount) {
			return new PlainBoxItemSession(copyBoxItems(initialBoxItems), order, containers, containerCount, interrupt);
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
	
	protected class PlainBoxItemGroupSession extends AbstractBoxItemGroupSession {

		public PlainBoxItemGroupSession(List<BoxItemGroup> boxItemGroups,
				Order order,
				List<ContainerItem> containers,
				int containerCount, PackagerInterruptSupplier interrupt) {
			super(boxItemGroups, containers, containerCount, order, interrupt);
		}

		private PlainBoxItemGroupSession(PlainBoxItemGroupSession source) {
			super(source);
		}

		@Override
		public PackagerSession fork() {
			return new PlainBoxItemGroupSession(this);
		}

		@Override
		protected PlainBoxItemGroupSession fresh(List<ContainerItem> containers, int containerCount) {
			return new PlainBoxItemGroupSession(copyBoxItemGroups(initialBoxItemGroups), order, containers, containerCount, interrupt);
		}

		@Override
		protected IntermediatePackagerResult packGroup(List<BoxItemGroup> remainingBoxItemGroups, Order order,
				ContainerItem containerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			return PlainPackager.this.packGroup(remainingBoxItemGroups, order, containerItem, interrupt, abortOnAnyBoxTooBig, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, maxLoadIdenticalBoxCount);
		}
		
		@Override
		protected IntermediatePackagerResult copy(ContainerItem controlledContainerItem, IntermediatePackagerResult result, int index) {
			return createIntermediatePackagerResult(controlledContainerItem, result.getStack());
		}

	}
	
	@Override
	protected PackagerSession newSession(PackagerInput input, PackagerInterruptSupplier interrupt) {
		if(input.hasBoxItems()) {
			return new PlainBoxItemSession(input.getBoxItems(), input.getOrder(), input.getContainerItems(), input.getMaxContainerCount(), interrupt);
		}
		return new PlainBoxItemGroupSession(input.getBoxItemGroups(), input.getOrder(), input.getContainerItems(), input.getMaxContainerCount(), interrupt);
	}

	public class PlainResultBuilder extends AbstractPackagerResultBuilder<PlainResultBuilder> {

		@Override
		public PackagerResult build() {
			return pack(validate(PlainPackager.this), deadline, interrupt);
		}
	}

	public static class Builder {

		// only applies if no placementControlsBuilderFactory is provided
		protected boolean requireFullSupport;
		protected boolean calculateSupport;
		
		protected IntermediatePackagerResultComparator packagerResultComparator;
		protected BoxItemGroupComparator boxItemGroupComparator;
		protected PlacementControlsBuilderFactory placementControlsBuilderFactory;
		protected ContainerPackingStrategyFactory containerPackingStrategyFactory;
		
		public Builder withCalculateSupport(boolean calculateSupport) {
			this.calculateSupport = calculateSupport;
			return this;
		}
		
		public Builder withRequireFullSupport(boolean requireFullSupport) {
			this.requireFullSupport = requireFullSupport;
			return this;
		}

		
		public Builder withBoxItemGroupComparator(BoxItemGroupComparator comparator) {
			this.boxItemGroupComparator = comparator;
			return this;
		}
		
		public Builder withIntermediatePackagerResultComparator(IntermediatePackagerResultComparator comparator) {
			this.packagerResultComparator = comparator;
			return this;
		}
		
		/**
		 * Set the factory which selects the container packing strategy: which containers to use, and in which order.
		 * By default, cost-aware packing is used when the containers have costs, otherwise the first container
		 * (in preference order) which holds the boxes.
		 *
		 * @param factory container packing strategy factory
		 * @return this builder
		 */
		public Builder withContainerPackingStrategyFactory(ContainerPackingStrategyFactory factory) {
			this.containerPackingStrategyFactory = Objects.requireNonNull(factory);
			return this;
		}

		public Builder withPlacementControlsBuilderFactory(PlacementControlsBuilderFactory factory) {
			this.placementControlsBuilderFactory = factory;
			return this;
		}

		/**
		 * Configure the placement controls with a consumer, to customize the ranking of boxes and positions without creating the
		 * controls factory yourself. The consumer's {@code withCalculateSupport(..)} and {@code withRequireFullSupport(..)} have the
		 * same effect as this builder's options of the same name, which must not be set as well.
		 *
		 * @param consumer configures the placement controls
		 * @return this builder
		 */
		public Builder withPlacementControlsBuilderFactory(Consumer<PlacementControlsBuilderFactoryBuilder> consumer) {
			PlacementControlsBuilderFactoryBuilder b = new PlacementControlsBuilderFactoryBuilder();
			consumer.accept(b);

			this.placementControlsBuilderFactory = b.build();
			return this;
		}

		public PlainPackager build() {
			if(placementControlsBuilderFactory != null && (requireFullSupport || calculateSupport)) {
				throw new IllegalStateException("Support options only apply to the default placement controls: configure support with the placement controls");
			}
			IntermediatePackagerResultComparator packagerResultComparator = this.packagerResultComparator;
			if(packagerResultComparator == null) {
				packagerResultComparator = new DefaultIntermediatePackagerResultComparator();
			}
			PlacementControlsBuilderFactory placementControlsBuilderFactory = this.placementControlsBuilderFactory;
			if(placementControlsBuilderFactory == null) {
				placementControlsBuilderFactory = new PlacementControlsBuilderFactoryBuilder()
						.withCalculateSupport(calculateSupport)
						.withRequireFullSupport(requireFullSupport)
						.build();
			}
			BoxItemGroupComparator boxItemGroupComparator = this.boxItemGroupComparator;
			if(boxItemGroupComparator == null) {
				boxItemGroupComparator = VolumeThenWeightBoxItemGroupComparator.getInstance();
			}
			PlainPackager packager = createPackager(packagerResultComparator, boxItemGroupComparator, placementControlsBuilderFactory);
			if(containerPackingStrategyFactory != null) {
				packager.setContainerPackingStrategyFactory(containerPackingStrategyFactory);
			}
			return packager;
		}

		/**
		 * Create the packager from the options of this builder, with defaults for the options which are not set. Override to
		 * create a subclass of the packager which has the same defaults.
		 *
		 * @param packagerResultComparator the result comparator
		 * @param boxItemGroupComparator the box item group comparator
		 * @param placementControlsBuilderFactory the placement controls
		 * @return a new packager
		 */
		protected PlainPackager createPackager(IntermediatePackagerResultComparator packagerResultComparator, BoxItemGroupComparator boxItemGroupComparator,
				PlacementControlsBuilderFactory placementControlsBuilderFactory) {
			return new PlainPackager(packagerResultComparator, boxItemGroupComparator, placementControlsBuilderFactory);
		}
		
	}

	protected PlacementControlsBuilderFactory placementControlsBuilderFactory;
	protected BoxItemGroupComparator boxItemGroupComparator;

	public PlainPackager(IntermediatePackagerResultComparator comparator, BoxItemGroupComparator boxItemGroupComparator, PlacementControlsBuilderFactory placementControlsBuilderFactory) {
		super(comparator);

		this.placementControlsBuilderFactory = placementControlsBuilderFactory;
		this.boxItemGroupComparator = boxItemGroupComparator;
	}

	protected BoxItemGroupIterator createBoxItemGroupIterator(BoxItemGroupSource boxItemGroupSource, Order order, Container container, PointCalculator pointCalculator) {
		if(order == Order.CHRONOLOGICAL || order == Order.CHRONOLOGICAL_ALLOW_SKIPPING) {
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

	@Override
	public String getUnsupportedReason(PackagerInput input) {
		String reason = super.getUnsupportedReason(input);
		if(reason != null) {
			return reason;
		}
		if(!placementControlsBuilderFactory.supportsLoad() && hasLoadLimits(input)) {
			return "Load limits not supported by the placement controls";
		}
		return null;
	}
}
