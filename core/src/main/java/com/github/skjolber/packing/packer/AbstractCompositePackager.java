package com.github.skjolber.packing.packer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;

/**
 * 
 * Combine multiple packagers in the same operations. Tiered strategy: Try to get the a baseline result using the first
 * tier, then improve on the result with the next tiers.
 * 
 * @param <B>
 */

public abstract class AbstractCompositePackager<B extends PackagerResultBuilder> extends AbstractPackager<B> {

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

	public AbstractCompositePackager(Comparator<IntermediatePackagerResult> comparator, List<PackagerTier> packagers) {
		super(comparator);
		this.packagers = packagers;
	}

	public class CompositeResultBuilder extends AbstractPackagerResultBuilder<CompositeResultBuilder> {

		@Override
		public PackagerResult build() {
			validate();

			long start = System.currentTimeMillis();

			PackagerInterruptSupplierBuilder booleanSupplierBuilder = PackagerInterruptSupplierBuilder.builder();
			if(deadline != -1L) {
				booleanSupplierBuilder.withDeadline(deadline);
			}
			if(interrupt != null) {
				booleanSupplierBuilder.withInterrupt(interrupt);
			}

			booleanSupplierBuilder.withScheduledThreadPoolExecutor(getScheduledThreadPoolExecutor());

			PackagerInterruptSupplier interruptSupplier = booleanSupplierBuilder.build();

			ContainerItemsCalculator containerItemsCalculator = new ContainerItemsCalculator(containers);
			try {
				List<Container> bestResult = null;

				tiers:
				for(PackagerTier tier : packagers) {
					for(PackagerAdapterBuilderFactory factory : tier.getPackagers()) {
						if(interruptSupplier.getAsBoolean()) {
							break tiers;
						}

						PackagerAdapter adapter = factory.newPackagerAdapterBuilder()
							.withBoxItemGroups(itemGroups)
							.withBoxItems(items)
							.withOrder(order)
							.withInterrupt(interruptSupplier)
							.withContainerItemsCalculator(containerItemsCalculator)
							.build();

						try {
							List<Container> result = packAdapter(maxContainerCount, interruptSupplier, adapter);
							if(result != null && !result.isEmpty()) {
								if(bestResult == null || result.size() < bestResult.size()) {
									bestResult = result;
								}
							}
						} catch(PackagerInterruptedException e) {
							// use best result so far and stop
							break tiers;
						}
					}
				}

				if(bestResult == null) {
					bestResult = Collections.emptyList();
				}

				long duration = System.currentTimeMillis() - start;
				return new PackagerResult(bestResult, duration, false);
			} finally {
				interruptSupplier.close();
			}
		}
	}

	@Override
	protected IntermediatePackagerResult createEmptyIntermediatePackagerResult() {
		return EmptyIntermediatePackagerResult.EMPTY;
	}

	@Override
	public void close() {
		super.close();
		for(PackagerTier tier : packagers) {
			for(PackagerAdapterBuilderFactory factory : tier.getPackagers()) {
				try {
					factory.close();
				} catch(IOException e) {
					// ignore
				}
			}
		}
	}

}
