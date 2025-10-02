package com.github.skjolber.packing.jmh;

import java.util.List;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.packer.AbstractPackager;

@SuppressWarnings("rawtypes")
public class BenchmarkSet {

	private final List<ContainerItem> containers;
	private final List<BoxItem> products;
	private final AbstractPackager packager;

	public BenchmarkSet(AbstractPackager packager, List<BoxItem> products, List<ContainerItem> containers) {
		this.packager = packager;
		this.products = products;
		this.containers = containers;
	}

	public boolean add(BoxItem arg0) {
		return products.add(arg0);
	}

	public List<BoxItem> getProducts() {
		return products;
	}

	public AbstractPackager getPackager() {
		return packager;
	}

	public List<ContainerItem> getContainers() {
		return containers;
	}
}
