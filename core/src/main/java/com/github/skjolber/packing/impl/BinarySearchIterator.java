package com.github.skjolber.packing.impl;

public class BinarySearchIterator {

	private int low;
	private int high;
	private int mid;

	BinarySearchIterator(int low, int high) {
		super();
		this.low = low;
		this.high = high;
	}

	public BinarySearchIterator() {
	}

	public int next() {
		return mid = low + (high - low) / 2;
	}

	public void lower() {
		high = mid - 1;
	}

	public void higher() {
		low = mid + 1;
	}

	public boolean hasNext() {
		return low <= high;
	}

	public void reset(int high, int low) {
		this.high = high;
		this.low = low;
	}

}
