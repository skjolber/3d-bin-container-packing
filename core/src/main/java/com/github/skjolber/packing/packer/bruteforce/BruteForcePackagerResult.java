package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.iterator.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;
import com.github.skjolber.packing.packer.PackResult;
import com.github.skjolber.packing.points2d.Point2D;

public class BruteForcePackagerResult<P extends Point2D> implements PackResult {

	private List<StackPlacement> placements = new ArrayList<>();
	private Container container;
	private DefaultPermutationRotationIterator iterator;
	private PermutationRotationState state;

	public BruteForcePackagerResult(Container container, DefaultPermutationRotationIterator iterator) {
		super();
		this.container = container;
		this.iterator = iterator;
	}

	public Container getContainer() {
		Stack stack = container.getStack();
		stack.clear();
		stack.addAll(placements);
		
		return container;
	}

	public DefaultPermutationRotationIterator getPermutationRotationIteratorForState() {
		iterator.setState(state);
		return iterator;
	}
	
	public void setState(List<StackPlacement> items, int length, PermutationRotationState state) {
		List<StackPlacement> clone = new ArrayList<>(length);
		for (int i = 0; i < length; i++) {
			StackPlacement stackPlacement = items.get(i);
			clone.add(stackPlacement.clone());
		}
		this.placements = clone;
		this.state = state;
	}

	@Override
	public boolean packsMoreBoxesThan(PackResult result) {
		// return true if 'this' is better:
		// - higher number of boxes
		// - lower volume
		// - lower max weight

		BruteForcePackagerResult<P> bruteForceResult = (BruteForcePackagerResult<P>)result;
		if(bruteForceResult.getPlacementCount() < placements.size()) {
			return true;
		} else if(bruteForceResult.getPlacementCount() == placements.size()) {
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

	@Override
	public boolean isEmpty() {
		return placements.isEmpty();
	}

	public int getPlacementCount() {
		return placements.size();
	}

}
