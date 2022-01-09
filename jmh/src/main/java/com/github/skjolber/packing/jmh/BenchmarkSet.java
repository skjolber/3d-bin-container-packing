package com.github.skjolber.packing.jmh;

import java.util.List;

import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.packer.AbstractPackager;

public class BenchmarkSet {

	private final List<StackableItem> products;
	private final AbstractPackager packager;
	
	public BenchmarkSet(AbstractPackager packager, List<StackableItem> products) {
		this.packager = packager;
		this.products = products;
	}
	
	public boolean add(StackableItem arg0) {
		return products.add(arg0);
	}
	public List<StackableItem> getProducts() {
		return products;
	}

	public AbstractPackager getPackager() {
		return packager;
	}
}
