package com.github.skjolber.packing.packer.bruteforce;

import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.api.packager.PackResult;
import com.github.skjolber.packing.iterator.PermutationRotation;
import com.github.skjolber.packing.iterator.PermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;

public class BruteForcePackagerResult implements PackResult {

	public static final BruteForcePackagerResult EMPTY = new BruteForcePackagerResult(null, -1, null);

	static {
		EMPTY.setState(Collections.emptyList(), null, Collections.emptyList());
	}

	// work objects
	private final Container container;
	private final PermutationRotationIterator iterator;
	private final int index;

	// state
	private PermutationRotationState state;
	private List<Point> points = Collections.emptyList();
	private List<Placement> placements = Collections.emptyList();

	private boolean dirty = true;

	private long loadVolume;
	private int loadWeight;

	public BruteForcePackagerResult(Container container, int index, PermutationRotationIterator iterator) {
		this.container = container;
		this.iterator = iterator;
		this.index = index;
	}

	private void calculateLoad() {
		if(dirty) {
			dirty = false;
			long loadVolume = 0;
			int loadWeight = 0;

			for (int i = 0; i < points.size(); i++) {
				PermutationRotation permutationRotation = iterator.get(i);
				BoxItem boxItem = permutationRotation.getBoxItem();
				Box box = boxItem.getBox();
				
				loadVolume += box.getVolume();
				loadWeight += box.getWeight();
			}

			this.loadVolume = loadVolume;
			this.loadWeight = loadWeight;
		}
	}

	public Container getContainer() {
		Stack stack = container.getStack();
		stack.clear();

		List<PermutationRotation> list = iterator.get(state, points.size());

		for (int i = 0; i < points.size(); i++) {
			Placement stackPlacement = placements.get(i);

			PermutationRotation permutationRotation = list.get(i);
			stackPlacement.setStackValue(permutationRotation.getBoxStackValue());

			Point point3d = points.get(i);
			stackPlacement.setPoint(point3d);

			stack.add(stackPlacement);
		}

		return container;
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

	@Override
	public boolean containsLastStackable() {
		return placements.size() == points.size();
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
		return container.getMaxLoadWeight();
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
		return container.getMaxLoadVolume();
	}

	public void markDirty() {
		this.dirty = true;
	}

	@Override
	public int getContainerItemIndex() {
		return index;
	}
}
