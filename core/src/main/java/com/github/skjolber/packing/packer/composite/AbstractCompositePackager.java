package com.github.skjolber.packing.packer.composite;

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
import com.github.skjolber.packing.packer.AbstractBoxItemAdapter;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.AbstractPackagerResultBuilder;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.ControlledContainerItem;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerAdapterBuilderFactory;
import com.github.skjolber.packing.packer.PackagerInterruptedException;
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

public abstract class AbstractCompositePackager<B extends PackagerResultBuilder> extends AbstractPackager<B> implements Packager<B> {

	protected List<PackagerAdapterBuilderFactory> packagers;
	protected PackagerAdapterCompositeManagerBuilderFactory managerBuilderFactory;

	public AbstractCompositePackager(Comparator<IntermediatePackagerResult> comparator, List<PackagerAdapterBuilderFactory> packagers, PackagerAdapterCompositeManagerBuilderFactory managerBuilderFactory) {
		super(comparator);
		
		this.packagers = packagers;
		this.managerBuilderFactory = managerBuilderFactory;
	}

	protected class CompositeBoxItemAdapter implements PackagerAdapter {

		private List<PackagerAdapter> adapters;
		private PackagerAdapterCompositeManager manager;
		
		@Override
		public IntermediatePackagerResult attempt(int containerIndex, IntermediatePackagerResult best,
				boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			
			IntermediatePackagerResult better = best;
			
			for (PackagerAdapter adapter : adapters) {
				IntermediatePackagerResult result = adapter.attempt(containerIndex, better, abortOnAnyBoxTooBig);
				if(result.isEmpty()) {
					continue;
				}
				if(intermediatePackagerResultComparator.compare(result, better) > 0) {
					better = result;
					
					if(manager.accept(containerIndex, better)) {
						break;
					}

					if(better.getStack().size() == adapter.countRemainingBoxes()) {
						break;
					}
				}
			}
			return better == best ? null : better;
		}
		@Override
		public IntermediatePackagerResult peek(int containerIndex, IntermediatePackagerResult existing) {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public Container accept(IntermediatePackagerResult result) {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public List<Integer> getContainers(int maxCount) {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public ContainerItem getContainerItem(int index) {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public int countRemainingBoxes() {
			// TODO Auto-generated method stub
			return 0;
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
				
				PackagerAdapterCompositeManager manager = managerBuilderFactory.createPackagerAdapterCompositeManagerBuilder()
						.withBoxItemGroups(itemGroups)
					.withBoxItems(items)
					.withOrder(order)
					.withContainerItemsCalculator(containerItemsCalculator)
					.withPackagerAdapters(adapters).build();

				PackagerAdapter compositeAdapter = createCompositeAdapter(adapters, manager);
				
				List<Container> packList = packAdapter(maxContainerCount, interrupt, compositeAdapter);
				
				long duration = System.currentTimeMillis() - start;
				return new PackagerResult(packList, duration, false);
			} catch (PackagerInterruptedException e) {
				long duration = System.currentTimeMillis() - start;
				return new PackagerResult(Collections.emptyList(), duration, true);
			} finally {
				interrupt.close();
			}
		}

		private PackagerAdapter createCompositeAdapter(List<PackagerAdapter> adapters, PackagerAdapterCompositeManager manager) {
			// return packagerAdapter that combines list into one, controlled by the manager. If a packagerAdapter is able to produce an acceptable result, skip the rest of the list. If not, try the next one, and so on.
			
			return null;
		}
	}
	
	@Override
	public void close() throws IOException {
		
		for (PackagerAdapterBuilderFactory f : packagers) {
			f.close();
		}
		
	}

}
