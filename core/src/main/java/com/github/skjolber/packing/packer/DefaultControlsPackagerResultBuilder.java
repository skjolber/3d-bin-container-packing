package com.github.skjolber.packing.packer;

import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.packer.strategy.ContainerResult;
import com.github.skjolber.packing.api.interrupt.DefaultPackagerInterrupt;

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
				adapter = createDefaultBoxItemAdapter(items, order, containers, maxContainerCount, interrupt);
			} else {
				adapter = createDefaultBoxItemGroupAdapter(itemGroups, order, containers, maxContainerCount, interrupt);
			}
			ContainerResult result = packager.packAdapter(interrupt, adapter);
			
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

	protected abstract PackagerAdapter createDefaultBoxItemAdapter(List<BoxItem> items, Order order,
			List<ControlledContainerItem> containers, int containerCount, PackagerInterruptSupplier interrupt);

	protected abstract PackagerAdapter createDefaultBoxItemGroupAdapter(List<BoxItemGroup> itemGroups, Order order,
			List<ControlledContainerItem> containers, int containerCount, PackagerInterruptSupplier interrupt);
}
