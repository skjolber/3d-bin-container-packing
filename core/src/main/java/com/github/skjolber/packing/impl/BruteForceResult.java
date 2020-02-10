package com.github.skjolber.packing.impl;

import java.util.List;

import com.github.skjolber.packing.BruteForcePackager;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.Placement;

public class BruteForceResult implements PackResult {

	private PermutationRotationIterator rotator;
	private List<Placement> items;
	private Container container;

	private int count;
	private PermutationRotationState state;

	public BruteForceResult(PermutationRotationIterator rotator, List<Placement> items, Container container) {
		super();
		this.rotator = rotator;
		this.container = container;
		this.items = items;
	}

	public boolean isRemainder() {
		return count < items.size();
	}

	public Container getContainer() {
		container.clear();
		if(state == null ) {
			throw new RuntimeException();
		}
		rotator.setState(state);

		int result = BruteForcePackager.pack(items, container, rotator, Long.MAX_VALUE, Integer.MAX_VALUE, container, 0);
		if(result == count) {
			return container;
		}
		throw new IllegalArgumentException("Unexpected count " + result + ", expected " + count);
	}

	public void setState(PermutationRotationState state) {
		this.state = state;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getCount() {
		return count;
	}

	@Override
	public boolean packsMoreBoxesThan(PackResult result) {
		// return true if 'this' is better:
		// - higher number of boxes
		// - lower volume
		// - lower max weight

		BruteForceResult bruteForceResult = (BruteForceResult)result;
		if(bruteForceResult.count < count) {
			return true;
		} else if(bruteForceResult.count == count) {
			// check volume (of container)
			if(bruteForceResult.container.getVolume() > container.getVolume()) {
				return true;
			} else if(bruteForceResult.container.getVolume() == container.getVolume()) {
				// check weight (max weight of container, suboptimal but quick)
				if(bruteForceResult.container.getWeight() > container.getWeight()) {
					return true;
				}
			}
		}

		return false;
	}

	public PermutationRotationIterator getRotator() {
		return rotator;
	}

	@Override
	public boolean isEmpty() {
		return count == 0;
	}

}
