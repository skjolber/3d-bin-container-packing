package com.github.skjolber.packing.packer.bruteforce;

import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackResult;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.iterator.PermutationRotation;
import com.github.skjolber.packing.iterator.PermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;

public class BruteForcePackagerResult implements PackResult {

	public static final BruteForcePackagerResult EMPTY = new BruteForcePackagerResult(null, null);
	
	static {
		EMPTY.setState(Collections.emptyList(), null, Collections.emptyList(), false);
	}
	
	// work objects
	private final Container container;
	private final PermutationRotationIterator iterator;
	
	// state
	private PermutationRotationState state;
	private List<Point3D<StackPlacement>> points = Collections.emptyList();
	private List<StackPlacement> placements;
	private boolean last;
	
	private boolean dirty = true;
	
	private long loadVolume;
	private int loadWeight;
	
	public BruteForcePackagerResult(Container container, PermutationRotationIterator iterator) {
		this.container = container;
		this.iterator = iterator;
	}
	
	private void calculateLoad() {
		if(dirty) {
			dirty = false;
			long loadVolume = 0;
			int loadWeight = 0;
	
			for(int i = 0; i < points.size(); i++) {
				PermutationRotation permutationRotation = iterator.get(i);
				Stackable stackable = permutationRotation.getStackable();
				loadVolume += stackable.getVolume();
				loadWeight += stackable.getWeight();
			}
	
			this.loadVolume = loadVolume;
			this.loadWeight = loadWeight;
		}
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

			Point3D<StackPlacement> point3d = points.get(i);
			stackPlacement.setX(point3d.getMinX());
			stackPlacement.setY(point3d.getMinY());
			stackPlacement.setZ(point3d.getMinZ());
			
			stack.add(stackPlacement);			
		}
		
		return container;
	}

	public PermutationRotationIterator getPermutationRotationIteratorForState() {
		iterator.setState(state);
		return iterator;
	}
	
	public void setState(List<Point3D<StackPlacement>> items, PermutationRotationState state, List<StackPlacement> placements, boolean last) {
		this.points = items;
		this.state = state;
		this.placements = placements;
		this.last = last;
		
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

	@Override
	public boolean containsLastStackable() {
		return last;
	}

	@Override
	public int getSize() {
		return points.size();
	}

	@Override
	public long getLoadVolume() {
		calculateLoad();
		return loadVolume;
	}

	@Override
	public int getLoadWeight() {
		calculateLoad();
		return loadWeight;
	}

	@Override
	public int getMaxLoadWeight() {
		return container.getStack().getContainerStackValue().getMaxLoadWeight();
	}

	@Override
	public int getWeight() {
		calculateLoad();
		return loadWeight + container.getEmptyWeight();
	}

	@Override
	public long getVolume() {
		return container.getVolume();
	}

	@Override
	public long getMaxLoadVolume() {
		return container.getStack().getContainerStackValue().getMaxLoadVolume();
	}

	public void markDirty() {
		this.dirty = true;
	}

}
