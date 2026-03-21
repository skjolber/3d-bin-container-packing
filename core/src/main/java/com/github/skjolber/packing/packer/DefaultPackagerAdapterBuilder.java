package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Order;

public class AbstractPackagerAdapterBuilder implements PackagerAdapterBuilder {

	protected BooleanSupplier interrupt;

	protected List<BoxItemGroup> itemGroups = new ArrayList<>();

	protected List<BoxItem> items = new ArrayList<>();
	
	protected ContainerItemsCalculator calcultor;
	
	protected Order order;

	@Override
	public PackagerAdapterBuilder withBoxItems(List<BoxItem> items) {
		this.items = items;
		return this;
	}

	@Override
	public PackagerAdapterBuilder withOrder(Order order) {
		this.order = order;
		return this;
	}

	@Override
	public PackagerAdapterBuilder withInterrupt(BooleanSupplier interrupt) {
		this.interrupt = interrupt;
		return this;
	}

	@Override
	public PackagerAdapterBuilder withContainerItemsCalculator(ContainerItemsCalculator calculator) {
		this.calcultor = calculator;
		return this;
	}

	@Override
	public PackagerAdapterBuilder withBoxItemGroups(List<BoxItemGroup> items) {
		this.itemGroups = items;
		return this;
	}

	@Override
	public PackagerAdapter build() {
		
		return this;
	}
	
	
	
}
