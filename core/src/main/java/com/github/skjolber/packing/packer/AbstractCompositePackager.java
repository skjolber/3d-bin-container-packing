package com.github.skjolber.packing.packer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemGroupComparator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.packer.plain.PlainPlacement;
import com.github.skjolber.packing.packer.plain.PlainPlacementComparator;
import com.github.skjolber.packing.packer.plain.PlainPlacementControlsBuilderFactory;
import com.github.skjolber.packing.packer.plain.PlainPackager.Builder;
import com.github.skjolber.packing.packer.plain.PlainPackager.PlainResultBuilder;
import com.github.skjolber.packing.packer.plain.PlainPackager.Builder.PlacementControlsBuilderFactoryBuilder;

/**
 * 
 * Combine multiple packagers in the same operations. Tiered strategy: Try to get the a baseline result using the first tier, then improve on the result with the next tiers.
 * 
 * @param <B>
 */

public abstract class AbstractCompositePackager<B extends PackagerResultBuilder> implements Packager<B> {

	protected final List<PackagerTier> packagers;
	
	protected static class PackagerTier {
		
		protected List<PackagerAdapterBuilderFactory> packagers;
		protected String name;
		
		public PackagerTier(String name, List<PackagerAdapterBuilderFactory> packagers) {
			this.name = name;
			this.packagers = packagers;
		}
		
		public String getName() {
			return name;
		}
		
		public List<PackagerAdapterBuilderFactory> getPackagers() {
			return packagers;
		}
		
	}

	protected class CompositeBoxItemAdapter extends AbstractBoxItemAdapter {

		public CompositeBoxItemAdapter(List<BoxItem> boxItems, Order order,
				ContainerItemsCalculator packagerContainerItems, PackagerInterruptSupplier interrupt) {
			super(boxItems, order, packagerContainerItems, interrupt);
		}

		@Override
		public IntermediatePackagerResult attempt(int containerIndex, IntermediatePackagerResult intermediatePackagerResult, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			return null;
		}

		@Override
		public Container accept(IntermediatePackagerResult result) {
			return null;
		}

		@Override
		protected IntermediatePackagerResult pack(List<BoxItem> remainingBoxItems,
				ControlledContainerItem containerItem, PackagerInterruptSupplier interrupt, Order order,
				boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			return null;
		}

		@Override
		protected IntermediatePackagerResult copy(ControlledContainerItem peek, IntermediatePackagerResult intermediatePackagerResult,
				int index) {
			return null;
		}

	}
	
	/*
	
	protected class CompositeBoxItemGroupAdapter extends AbstractBoxItemGroupAdapter {

		protected final List<PackagerAdapter> adapters;
		protected final PackagerInterruptSupplier interrupt;
		protected final ContainerItemsCalculator packagerContainerItems;
		
		public CompositeBoxItemGroupAdapter(List<PackagerAdapter> adapters,
				ContainerItemsCalculator packagerContainerItems,
				PackagerInterruptSupplier interrupt) {
			this.packagerContainerItems = packagerContainerItems;
			this.interrupt = interrupt;
			this.adapters = adapters;
		}

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
			
			@Override
			protected IntermediatePackagerResult copy(ControlledContainerItem controlledContainerItem, IntermediatePackagerResult result, int index) {
				return createIntermediatePackagerResult(controlledContainerItem, result.getStack());
			}

		}
	}	
	
	*/
	
	public class CompositeResultBuilder extends AbstractPackagerResultBuilder<PlainResultBuilder> {

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
			
			PackagerInterruptSupplier interrupt = booleanSupplierBuilder.build();
			
			ContainerItemsCalculator containerItemsCalculator = new ContainerItemsCalculator(containers);
			try {
				List<PackagerAdapter> adapters = new ArrayList<>();
				
				for (PackagerAdapterBuilderFactory f : packagers) {
					
					PackagerAdapter packagerAdapter = f.newPackagerAdapterBuilder()
						.withBoxItemGroups(itemGroups)
						.withBoxItems(items)
						.withOrder(order)
						.withInterrupt(interrupt)
						.withContainerItemsCalculator(containerItemsCalculator)
						.build();
					
					adapters.add(packagerAdapter);
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
	
	@Override
	public void close() throws IOException {
		
		for (PackagerAdapterBuilderFactory f : packagers) {
			f.close();
		}
		
	}

	@Override
	public B newResultBuilder() {
		// TODO Auto-generated method stub
		return null;
	}

}
