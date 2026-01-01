package com.github.skjolber.packing.packer;

import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;

public abstract class DefaultControlsPackagerResultBuilder extends AbstractPackagerResultBuilder<DefaultControlsPackagerResultBuilder> {
	
	private AbstractPackager<DefaultControlsPackagerResultBuilder> packager;

	public DefaultControlsPackagerResultBuilder withPackager(AbstractPackager<DefaultControlsPackagerResultBuilder> packager) {
		this.packager = packager;
		return this;
	}
	
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

		booleanSupplierBuilder.withScheduledThreadPoolExecutor(packager.getScheduledThreadPoolExecutor());

		PackagerInterruptSupplier interrupt = booleanSupplierBuilder.build();
		try {
			PackagerAdapter adapter;
			if(items != null && !items.isEmpty()) {
				adapter = createDefaultBoxItemAdapter(items, order, new ContainerItemsCalculator(containers), interrupt);
			} else {
				adapter = createDefaultBoxItemGroupAdapter(itemGroups, order, new ContainerItemsCalculator(containers), interrupt);
			}
			List<Container> packList = packager.packAdapter(maxContainerCount, interrupt, adapter);
			
			long duration = System.currentTimeMillis() - start;
			return new PackagerResult(packList, duration, false);
		} catch (PackagerInterruptedException e) {
			long duration = System.currentTimeMillis() - start;
			return new PackagerResult(Collections.emptyList(), duration, true);
		} finally {
			interrupt.close();
		}
	}

	protected abstract PackagerAdapter createDefaultBoxItemAdapter(List<BoxItem> items, Order order,
			ContainerItemsCalculator containerItemsCalculator, PackagerInterruptSupplier interrupt);

	protected abstract PackagerAdapter createDefaultBoxItemGroupAdapter(List<BoxItemGroup> itemGroups, Order order,
			ContainerItemsCalculator containerItemsCalculator, PackagerInterruptSupplier interrupt);
}