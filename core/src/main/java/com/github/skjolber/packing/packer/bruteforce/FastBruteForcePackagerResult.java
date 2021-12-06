package com.github.skjolber.packing.packer.bruteforce;

import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.iterator.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotation;
import com.github.skjolber.packing.iterator.PermutationRotationState;
import com.github.skjolber.packing.packer.PackResult;
import com.github.skjolber.packing.points3d.Point3D;

public class  FastBruteForcePackagerResult implements PackResult {

	public static final FastBruteForcePackagerResult EMPTY = new FastBruteForcePackagerResult(null, null);
	
	static {
		EMPTY.setState(Collections.emptyList(), null, Collections.emptyList(), false);
	}
	
	// work objects
	private final Container container;
	private final DefaultPermutationRotationIterator iterator;
	
	// state
	private PermutationRotationState state;
	private List<Point3D> points = Collections.emptyList();
	private List<StackPlacement> placements;
	private boolean last;

	public FastBruteForcePackagerResult(Container container, DefaultPermutationRotationIterator iterator) {
		this.container = container;
		this.iterator = iterator;
	}
	
	public Container getContainer() {
		Stack stack = container.getStack();
		stack.clear();
		
		iterator.setState(state);
		
		for(int i = 0; i < points.size(); i++) {
			StackPlacement stackPlacement = placements.get(i);

			PermutationRotation permutationRotation = iterator.get(i);
			stackPlacement.setValue(permutationRotation.getValue());
			stackPlacement.setStackable(permutationRotation.getStackable());

			Point3D point3d = points.get(i);
			stackPlacement.setX(point3d.getMinX());
			stackPlacement.setY(point3d.getMinY());
			stackPlacement.setZ(point3d.getMinZ());
			
			stack.add(stackPlacement);			
		}
		
		return container;
	}

	public DefaultPermutationRotationIterator getPermutationRotationIteratorForState() {
		iterator.setState(state);
		return iterator;
	}
	
	public void setState(List<Point3D> items, PermutationRotationState state, List<StackPlacement> placements, boolean last) {
		this.points = items;
		this.state = state;
		this.placements = placements;
		this.last = last;
	}

	@Override
	public boolean isBetterThan(PackResult result) {
		// return true if 'this' is better:
		// - higher number of boxes
		// - lower volume
		// - lower max weight

		FastBruteForcePackagerResult bruteForceResult = (FastBruteForcePackagerResult)result;
		if(bruteForceResult.points.size() < points.size()) {
			return true;
		} else if(bruteForceResult.points.size() == points.size()) {
			// check volume (of container)
			if(bruteForceResult.container.getVolume() > container.getVolume()) {
				// this instance packs more items with a lower volume
				return true;
			} else if(bruteForceResult.container.getVolume() == container.getVolume()) {
				// check weight (max weight of container, suboptimal but quick)
				if(bruteForceResult.container.getWeight() > container.getWeight()) {
					// this instance is best
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean isEmpty() {
		return points.isEmpty();
	}

	@Override
	public boolean containsLastStackable() {
		return last;
	}
	
	public int getSize() {
		return points.size();
	}

}
