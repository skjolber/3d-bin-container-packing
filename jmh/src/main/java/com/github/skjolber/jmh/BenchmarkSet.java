package com.github.skjolber.jmh;

import java.util.List;

import com.github.skjolber.packing.BoxItem;
import com.github.skjolber.packing.BruteForcePackager;
import com.github.skjolber.packing.BruteForcePackagerBuilder;

public class BenchmarkSet {

	private final List<BoxItem> products;
	private final BruteForcePackager bruteForcePackager;
	
	public BenchmarkSet(BruteForcePackagerBuilder builder, List<BoxItem> products) {
		this.bruteForcePackager = builder.build();
		this.products = products;
	}
	
	public boolean add(BoxItem arg0) {
		return products.add(arg0);
	}
	public List<BoxItem> getProducts() {
		return products;
	}

	public BruteForcePackager getBruteForcePackager() {
		return bruteForcePackager;
	}
}
