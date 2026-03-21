package com.github.skjolber.packing.packer.composite;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.PackagerAdapter;

public abstract class AbstractPackagerAdapterCompositeManagerBuilder<B extends AbstractPackagerAdapterCompositeManagerBuilder<B>> implements PackagerAdapterCompositeManagerBuilder<B> {

	protected List<BoxItemGroup> itemGroups = new ArrayList<>();

	protected List<BoxItem> items = new ArrayList<>();
	
	protected ContainerItemsCalculator calculator;
	
	protected Order order;

	protected List<PackagerAdapter> packagers;

	@Override
	public B withBoxItems(List<BoxItem> items) {
		this.items = items;
		return (B) this;
	}

	@Override
	public B withOrder(Order order) {
		this.order = order;
		return (B) this;
	}

	@Override
	public B withContainerItemsCalculator(ContainerItemsCalculator calculator) {
		this.calculator = calculator;
		return (B) this;
	}

	@Override
	public B withBoxItemGroups(List<BoxItemGroup> items) {
		this.itemGroups = items;
		return (B) this;
	}

	public B withPackagerAdapters(List<PackagerAdapter> packagers) {
		this.packagers = packagers;
		return (B) this;
	}
}
