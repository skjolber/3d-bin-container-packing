package com.github.skjolber.packing.test.generator.egy;

import com.github.skjolber.packing.test.generator.Item;

public class EgyItem extends Item {

	private final Category category;

	public EgyItem(int dx, int dy, int dz, int count, Category category) {
		super(dx, dy, dz, count);
		this.category = category;
	}
	
	public Category getCategory() {
		return category;
	}
	
	@Override
	public String toString() {
		return "Item [dx=" + dx + ", dy=" + dy + ", dz=" + dz + ", count=" + count + ", volume=" + volume
				+ ", category=" + category + "]";
	}
	
}
