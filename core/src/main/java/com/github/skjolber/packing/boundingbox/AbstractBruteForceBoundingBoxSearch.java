package com.github.skjolber.packing.boundingbox;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.PackagerInterruptedException;
import com.github.skjolber.packing.boundingbox.BruteForceBoundingBoxResult.Termination;
import com.github.skjolber.packing.packer.bruteforce.PointCalculator3DStack;

/** Operation-local exhaustive search; intentionally separate from the first-fit packing hot path. */
public abstract class AbstractBruteForceBoundingBoxSearch implements BruteForceBoundingBoxSearch {

	protected final List<BoxItem> items;
	protected final Container container;
	protected final PackagerInterruptSupplier interrupt;
	protected Map<BoxStackValue, BoxStackValue> originalValues = new IdentityHashMap<>();
	protected DefaultBoxItemPermutationRotationIterator iterator;
	protected PointCalculator3DStack points;
	protected Placement[] placements;
	protected BoxStackValue[] values;
	protected long[] minAreas;
	protected long[] minVolumes;
	protected final BoundingBoxLoadSupport loadSupport;

	protected AbstractBruteForceBoundingBoxSearch(List<BoxItem> items, Container container, PackagerInterruptSupplier interrupt, boolean load) {
		this.items = items;
		this.container = container;
		this.interrupt = interrupt;
		this.loadSupport = load ? new BoundingBoxLoadSupport(interrupt) : null;
	}

	@Override
	public BruteForceBoundingBoxResult pack(long start) {
		Termination termination = Termination.EXHAUSTED;
		try {
			checkInterrupt();
			if(prepare()) {
				search:
				do {
					do {
						checkInterrupt();
						prepareRotation();
						points.reset(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
						points.setMinimumAreaAndVolumeLimit(minAreas[0], minVolumes[0]);
						if(place(0, 0, 0, 0)) {
							termination = Termination.GOAL_REACHED;
							break search;
						}
					} while(iterator.nextRotation() != -1);
				} while(iterator.nextPermutation() != -1);
			}
		} catch (PackagerInterruptedException e) {
			termination = Termination.INTERRUPTED;
		}
		return result(termination, (System.nanoTime() - start) / 1_000_000L);
	}

	protected boolean prepare() {
		long remainingVolume = container.getMaxLoadVolume();
		long remainingWeight = container.getMaxLoadWeight();
		// Reject impossible complete assemblies before allocating per-physical-box buffers.
		// These bounded inventory/rotation scans do not poll; pack checks before
		// preparation and again before entering the combinatorial search.
		for(BoxItem item : items) {
			long volume = item.getBox().getVolume();
			long weight = item.getBox().getWeight();
			if(item.getCount() > remainingVolume / volume || (weight != 0 && item.getCount() > remainingWeight / weight)) {
				return false;
			}
			remainingVolume -= volume * item.getCount();
			remainingWeight -= weight * item.getCount();
		}

		BoxItem[] matrix = new BoxItem[items.size()];
		for(int index = 0; index < matrix.length; index++) {
			BoxItem item = items.get(index);
			List<BoxStackValue> rotations = new ArrayList<>();
			for(BoxStackValue original : item.getBox().getStackValues()) {
				if(original.fitsInside3D(container)) {
					BoxStackValue copy = original.clone();
					rotations.add(copy);
					originalValues.put(copy, original);
				}
			}
			if(rotations.isEmpty()) {
				return false;
			}
			// The iterator may mutate local indexes/state; nothing points back to a mutable input inventory.
			matrix[index] = new BoxItem(new Box(item.getBox(), rotations), item.getCount(), index, item.getGlobalIndex());
		}
		iterator = new DefaultBoxItemPermutationRotationIterator(matrix, List.of());
		int count = iterator.length();
		points = new PointCalculator3DStack(count + 1);
		placements = new Placement[count];
		for(int index = 0; index < count; index++) {
			placements[index] = new Placement(false);
		}
		values = new BoxStackValue[count];
		minAreas = new long[count];
		minVolumes = new long[count];
		return true;
	}

	protected void prepareRotation() {
		long minArea = Long.MAX_VALUE;
		long minVolume = Long.MAX_VALUE;
		for(int index = values.length - 1; index >= 0; index--) {
			BoxStackValue value = iterator.getStackValue(index);
			values[index] = value;
			minAreas[index] = minArea = Math.min(minArea, value.getArea());
			minVolumes[index] = minVolume = Math.min(minVolume, value.getVolume());
		}
	}

	protected boolean place(int depth, int dx, int dy, int dz) throws PackagerInterruptedException {
		BoxStackValue value = values[depth];
		Placement placement = placements[depth];
		placement.setStackValue(value);
		boolean last = depth + 1 == values.length;
		if(!last) {
			points.push();
		}
		try {
			int pointCount = points.size();
			for(int index = 0; index < pointCount; index++) {
				// Each candidate can expand an entire recursive placement subtree.
				checkInterrupt();
				SimplePoint3D point = points.get(index);
				if(!point.fits3D(value)) {
					continue;
				}
				int nextDx = Math.max(dx, point.getMinX() + value.getDx());
				int nextDy = Math.max(dy, point.getMinY() + value.getDy());
				int nextDz = Math.max(dz, point.getMinZ() + value.getDz());
				// Extents can only grow. Prune only if EVERY objective loses strictly;
				// equal leading dimensions can still improve an objective's tie-breakers.
				if(cannotImprove(nextDx, nextDy, nextDz)) {
					continue;
				}
				placement.setPoint(point);
				if(last) {
					if(complete(nextDx, nextDy, nextDz)) {
						return true;
					}
				} else {
					points.setMinimumAreaAndVolumeLimit(minAreas[depth + 1], minVolumes[depth + 1]);
					points.add(index, placement);
					if(place(depth + 1, nextDx, nextDy, nextDz)) {
						return true;
					}
					points.redo();
				}
			}
			return false;
		} finally {
			if(!last) {
				points.pop();
			}
		}
	}

	protected abstract boolean complete(int dx, int dy, int dz) throws PackagerInterruptedException;

	protected abstract boolean cannotImprove(int dx, int dy, int dz);

	protected abstract BruteForceBoundingBoxResult result(Termination termination, long duration);

	protected boolean isValidLayout() throws PackagerInterruptedException {
		return loadSupport == null || loadSupport.isValidLayout(placements, originalValues);
	}

	protected Stack createSnapshot() {
		return loadSupport == null ? copyPlacements(false) : loadSupport.createSnapshot(placements, originalValues);
	}

	protected Stack copyPlacements(boolean load) {
		Stack snapshot = new Stack(placements.length);
		for(int index = 0; index < placements.length; index++) {
			Placement current = placements[index];
			Placement copy = new Placement(originalValues.get(current.getStackValue()), current.getPointIndex(),
					current.getAbsoluteX(), current.getAbsoluteY(), current.getAbsoluteZ(), load);
			copy.setIndex(index);
			snapshot.add(copy);
		}
		return snapshot;
	}

	protected void checkInterrupt() throws PackagerInterruptedException {
		if(interrupt.getAsBoolean()) {
			throw new PackagerInterruptedException();
		}
	}
}
