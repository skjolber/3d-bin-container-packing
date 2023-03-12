package com.github.skjolber.packing.api;

public class ContainerInventoryItem {

	private int count;
	private final Container container;
	private final int index;

	public ContainerInventoryItem(Container container, int count, int index) {
		super();
		this.container = container;
		this.count = count;
		this.index = index;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public Container getContainer() {
		return container;
	}

	public int getIndex() {
		return index;
	}
	
	public boolean isAvailable() {
		return count > 0;
	}

	public void consume() {
		count--;
	}
	
	
}
