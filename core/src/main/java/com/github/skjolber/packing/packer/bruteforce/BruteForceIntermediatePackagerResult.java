package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;

public class BruteForceIntermediatePackagerResult implements IntermediatePackagerResult {
	
	private static final Comparator<Placement> ABSOLUTE_Z_COMPARATOR = Comparator.comparingInt(Placement::getAbsoluteZ);
	private static final Placement[] EMPTY_PLACEMENTS = new Placement[0];
	private static final byte STACK_DIRTY = 1;
	private static final byte CONTAINS_LAST_STACKABLE = 1 << 1;
	// after the constants used by instances
	public static final BruteForceIntermediatePackagerResult EMPTY = new BruteForceIntermediatePackagerResult(null, null, 0, null, false);

	// work objects
	private final Stack stack;
	private final ContainerItem containerItem;
	private final BoxItemPermutationRotationIterator iterator;
	private final int index;
	private final boolean calculateLoads;

	// state
	private PermutationRotationState state;
	private List<Point> points = Collections.emptyList();
	private ArrayList<Point> pointBuffer;
	private Placement[] placements = EMPTY_PLACEMENTS;
	private ArrayList<Placement> loadOrder;

	private byte flags = STACK_DIRTY;
	/**
	 * Whether the result may hold any of the session's remaining boxes, rather than the first in the session's order:
	 * for another order of the box item groups (see {@link AbstractBruteForceBoxItemGroupSession#attemptGroupOrders}),
	 * or when skipping boxes (see {@link AbstractBruteForcePackager#packInOrderSkipping}). Such results are accepted like
	 * results from other packagers.
	 */
	private boolean anyRemaining;

	private long loadVolume;
	private int loadWeight;

	public BruteForceIntermediatePackagerResult(ContainerItem containerItem, Stack stack, int index, BoxItemPermutationRotationIterator iterator) {
		this(containerItem, stack, index, iterator, true);
	}

	BruteForceIntermediatePackagerResult(ContainerItem containerItem, Stack stack, int index, BoxItemPermutationRotationIterator iterator, boolean calculateLoads) {
		this.containerItem = containerItem;
		this.stack = stack;
		this.iterator = iterator;
		this.index = index;
		this.calculateLoads = calculateLoads;
	}
	
	public void calculateWeightAndVolume() {
		long loadVolume = 0;
		int loadWeight = 0;

		if(state != null) {
			int[] permutations = state.getPermutations();
			BoxItem[] boxItems = iterator.getBoxItems();
			for(int i = 0; i < points.size(); i++) {
				Box box = boxItems[permutations[i]].getBox();
			
				loadVolume += box.getVolume();
				loadWeight += box.getWeight();
			}
		}

		this.loadVolume = loadVolume;
		this.loadWeight = loadWeight;
	}


	public void calculateLoad() {
		if((flags & STACK_DIRTY) != 0) {
			calculateStack();
			flags &= ~STACK_DIRTY;
		}
	}
	
	@Override
	public Stack getStack() {
		calculateLoad();

		return stack;
	}

	public void calculateStack() {
		stack.clear();
		if(points.isEmpty()) {
			return;
		}

		int[] permutations = state.getPermutations();
		
		List<BoxStackValue> list = iterator.get(state, points.size());
		
		for (int i = 0; i < points.size(); i++) {
			Placement stackPlacement = placements[i];

			BoxStackValue value = list.get(i);
			
			if(value.getBox().getBoxItem().getLocalIndex() != permutations[i]) {
				throw new RuntimeException();
			}
			
			stackPlacement.setStackValue(value);

			Point point3d = points.get(i);
			stackPlacement.setPoint(point3d);

			stack.add(stackPlacement);
		}

		if(calculateLoads) {
			rebuildLoads(stack.getPlacements());
		}
	}

	void rebuildLoads(List<Placement> placements) {
		for (int i = 0; i < placements.size(); i++) {
			Placement placement = placements.get(i);
			placement.clearLoad();
			placement.setIndex(i);
		}

		if(loadOrder == null) {
			loadOrder = new ArrayList<>(placements.size());
		} else {
			loadOrder.clear();
		}
		loadOrder.addAll(placements);
		loadOrder.sort(ABSOLUTE_Z_COMPARATOR);
		for (int i = 0; i < loadOrder.size(); i++) {
			addLoad(loadOrder.get(i), loadOrder, i);
		}
	}

	private static void addLoad(Placement placement, List<Placement> placements, int limit) {
		if(placement.getAbsoluteZ() == 0) {
			return;
		}

		long totalArea = 0;
		for (int i = 0; i < limit; i++) {
			Placement supporter = placements.get(i);
			if(supporter.getAbsoluteEndZ() == placement.getAbsoluteZ() - 1 && supporter.intersects2D(placement)) {
				totalArea += supporter.overlapArea2D(placement);
			}
		}
		if(totalArea == 0) {
			return;
		}

		for (int i = 0; i < limit; i++) {
			Placement supporter = placements.get(i);
			if(supporter.getAbsoluteEndZ() == placement.getAbsoluteZ() - 1 && supporter.intersects2D(placement)) {
				long area = supporter.overlapArea2D(placement);
				supporter.addLoad(placement, area, (double) placement.getWeight() * area / totalArea);
			}
		}
	}

	@Override
	public ContainerItem getContainerItem() {
		return containerItem;
	}

	public PermutationRotationState getPermutationRotationIteratorForState() {
		return state;
	}

	/**
	 * @param containerItem another container which holds the same placements
	 * @param index the container's index
	 * @return the same packing in the other container
	 */
	BruteForceIntermediatePackagerResult copyTo(ContainerItem containerItem, int index) {
		BruteForceIntermediatePackagerResult copy = new BruteForceIntermediatePackagerResult(containerItem, new Stack(points.size()), index, iterator, calculateLoads);
		copy.points = new ArrayList<>(points);
		copy.state = state;
		copy.placements = placements;
		copy.loadVolume = loadVolume;
		copy.loadWeight = loadWeight;
		copy.flags = (byte)(STACK_DIRTY | (flags & CONTAINS_LAST_STACKABLE));
		copy.anyRemaining = anyRemaining;
		return copy;
	}

	public void setState(List<Point> items, PermutationRotationState state, Placement[] placements, int placementCount) {
		this.points = items;
		setState(state, placements, placementCount);
	}

	void setStateFromReusablePoints(List<Point> items, PermutationRotationState state, Placement[] placements, int placementCount) {
		int size = items.size();
		if(pointBuffer == null) {
			pointBuffer = new ArrayList<>(size);
		} else {
			pointBuffer.clear();
			pointBuffer.ensureCapacity(size);
		}
		for(int i = 0; i < size; i++) {
			pointBuffer.add(items.get(i));
		}
		this.points = pointBuffer;
		setState(state, placements, placementCount);
	}

	private void setState(PermutationRotationState state, Placement[] placements, int placementCount) {
		this.state = state;
		this.placements = placements;
		calculateWeightAndVolume();
		this.flags = (byte)(STACK_DIRTY | (placementCount == points.size() ? CONTAINS_LAST_STACKABLE : 0));
	}

	public void reset() {
		this.points = Collections.emptyList();
		if(pointBuffer != null) {
			pointBuffer.clear();
		}
		this.state = null;
		this.placements = EMPTY_PLACEMENTS;
		if(loadOrder != null) {
			loadOrder.clear();
		}
		this.loadVolume = 0;
		this.loadWeight = 0;
		this.flags = STACK_DIRTY;
	}

	@Override
	public boolean isEmpty() {
		return points.isEmpty();
	}

	public boolean isAnyRemaining() {
		return anyRemaining;
	}

	void setAnyRemaining(boolean anyRemaining) {
		this.anyRemaining = anyRemaining;
	}

	public boolean containsLastStackable() {
		return (flags & CONTAINS_LAST_STACKABLE) != 0;
	}

	public int getSize() {
		return points.size();
	}

	@Override
	public long getLoadVolume() {
		return loadVolume;
	}

	public int getLoadWeight() {
		return loadWeight;
	}

	public int getMaxLoadWeight() {
		return containerItem.getContainer().getMaxLoadWeight();
	}

	public int getWeight() {
		return loadWeight + containerItem.getContainer().getEmptyWeight();
	}

	public long getVolume() {
		return containerItem.getContainer().getVolume();
	}

	public long getMaxLoadVolume() {
		return containerItem.getContainer().getMaxLoadVolume();
	}

	public void markDirty() {
		this.flags |= STACK_DIRTY;
	}

	public int getContainerItemIndex() {
		return index;
	}
	
	public BoxItemPermutationRotationIterator getIterator() {
		return iterator;
	}

	boolean isCalculateLoads() {
		return calculateLoads;
	}
	
	public boolean isDirty() {
		return (flags & STACK_DIRTY) != 0;
	}

	public void trimToSize(int size) {
		if(size < points.size()) {
			flags &= ~CONTAINS_LAST_STACKABLE;
		}
		while (size < points.size()) {
			points.remove(points.size() - 1);
		}
		if(!stack.isEmpty()) {
			stack.setSize(size);
		}
		calculateWeightAndVolume();
		flags |= STACK_DIRTY;
	}

}
