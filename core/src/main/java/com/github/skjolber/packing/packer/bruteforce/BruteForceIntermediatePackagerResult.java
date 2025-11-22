package com.github.skjolber.packing.packer.bruteforce;

import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;
import com.github.skjolber.packing.packer.ControlledContainerItem;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;

public class BruteForceIntermediatePackagerResult implements IntermediatePackagerResult {
	
	public static final BruteForceIntermediatePackagerResult EMPTY = new BruteForceIntermediatePackagerResult(null, null, 0, null);

	// work objects
	private final Stack stack;
	private final ControlledContainerItem containerItem;
	private final BoxItemPermutationRotationIterator iterator;
	private final int index;

	// state
	private PermutationRotationState state;
	private List<Point> points = Collections.emptyList();
	private List<Placement> placements = Collections.emptyList();

	private boolean dirty = true;

	private long loadVolume;
	private int loadWeight;

	public BruteForceIntermediatePackagerResult(ControlledContainerItem containerItem, Stack stack, int index, BoxItemPermutationRotationIterator iterator) {
		this.containerItem = containerItem;
		this.stack = stack;
		this.iterator = iterator;
		this.index = index;
	}

	private void calculateLoad() {
		if(dirty) {
			dirty = false;
			
			calculateStack();
			
			long loadVolume = 0;
			int loadWeight = 0;

			for (int i = 0; i < points.size(); i++) {
				Placement stackPlacement = placements.get(i);
				
				BoxStackValue v = stackPlacement.getStackValue();
				Box box = v.getBox();
				
				loadVolume += box.getVolume();
				loadWeight += box.getWeight();
			}

			this.loadVolume = loadVolume;
			this.loadWeight = loadWeight;
		}
	}
	
	@Override
	public Stack getStack() {
		calculateLoad();

		return stack;
	}

	public void calculateStack() {
		stack.clear();

		int[] permutations = state.getPermutations();
		
		List<BoxStackValue> list = iterator.get(state, points.size());
		
		for (int i = 0; i < points.size(); i++) {
			Placement stackPlacement = placements.get(i);

			BoxStackValue value = list.get(i);
			
			if(value.getBox().getBoxItem().getIndex() != permutations[i]) {
				throw new RuntimeException();
			}
			
			stackPlacement.setStackValue(value);

			Point point3d = points.get(i);
			stackPlacement.setPoint(point3d);

			stack.add(stackPlacement);
		}
	}

	public ControlledContainerItem getContainerItem() {
		return containerItem;
	}

	public PermutationRotationState getPermutationRotationIteratorForState() {
		return state;
	}

	public void setState(List<Point> items, PermutationRotationState state, List<Placement> placements) {
		this.points = items;
		this.state = state;
		this.placements = placements;
		this.dirty = true;
	}

	public void reset() {
		this.points = Collections.emptyList();
		this.state = null;
		this.placements = Collections.emptyList();
		this.dirty = true;
	}

	@Override
	public boolean isEmpty() {
		return points.isEmpty();
	}

	public boolean containsLastStackable() {
		return placements.size() == points.size();
	}

	public int getSize() {
		return points.size();
	}

	public long getLoadVolume() {
		calculateLoad();
		return loadVolume;
	}

	public int getLoadWeight() {
		calculateLoad();
		return loadWeight;
	}

	public int getMaxLoadWeight() {
		return containerItem.getContainer().getMaxLoadWeight();
	}

	public int getWeight() {
		calculateLoad();
		return loadWeight + containerItem.getContainer().getEmptyWeight();
	}

	public long getVolume() {
		return containerItem.getContainer().getVolume();
	}

	public long getMaxLoadVolume() {
		return containerItem.getContainer().getMaxLoadVolume();
	}

	public void markDirty() {
		this.dirty = true;
	}

	public int getContainerItemIndex() {
		return index;
	}
	
	public BoxItemPermutationRotationIterator getIterator() {
		return iterator;
	}

}
