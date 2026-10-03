package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;

/**
 * Stack placements ordered by their top (end z), so that the supported area of a candidate only
 * visits the placements directly below it rather than the whole stack.
 * <p>
 * The index follows the stack passed to each query: placements appended to the stack are inserted,
 * any other change rebuilds the index.
 */
public class StackSupportIndex {

	private Placement[] placements = new Placement[16];
	private int[] endZ = new int[16];
	private int size;

	// the stack prefix which is indexed
	private int indexedCount;
	private Placement lastIndexed;

	private void synchronize(List<Placement> stack) {
		int stackSize = stack.size();
		if(stackSize == indexedCount && (stackSize == 0 || stack.get(stackSize - 1) == lastIndexed)) {
			return;
		}
		if(indexedCount > stackSize || (indexedCount > 0 && stack.get(indexedCount - 1) != lastIndexed)) {
			size = 0;
			indexedCount = 0;
		}
		if(placements.length < stackSize) {
			int capacity = Math.max(stackSize, placements.length * 2);
			Placement[] nextPlacements = new Placement[capacity];
			System.arraycopy(placements, 0, nextPlacements, 0, size);
			int[] nextEndZ = new int[capacity];
			System.arraycopy(endZ, 0, nextEndZ, 0, size);
			placements = nextPlacements;
			endZ = nextEndZ;
		}
		for (int i = indexedCount; i < stackSize; i++) {
			insert(stack.get(i));
		}
		indexedCount = stackSize;
		lastIndexed = stackSize > 0 ? stack.get(stackSize - 1) : null;
	}

	private void insert(Placement placement) {
		int value = placement.getAbsoluteEndZ();
		// after equal values, keeping stack order within a level
		int index = size;
		while (index > 0 && endZ[index - 1] > value) {
			index--;
		}
		System.arraycopy(placements, index, placements, index + 1, size - index);
		System.arraycopy(endZ, index, endZ, index + 1, size - index);
		placements[index] = placement;
		endZ[index] = value;
		size++;
	}

	/**
	 * Same result as {@link com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControls#calculateAreaSupport(List, int, int, int, BoxStackValue)}
	 * (assuming placements in the stack do not overlap).
	 */
	public long calculateAreaSupport(List<Placement> stack, int minX, int minY, int minZ, BoxStackValue stackValue) {
		synchronize(stack);
		long sum = 0;

		int maxX = minX + stackValue.getDx() - 1; // inclusive
		int maxY = minY + stackValue.getDy() - 1; // inclusive

		long max = (maxX - minX + 1) * (maxY - minY + 1);

		int z = minZ - 1;

		// first placement with its top at z
		int low = 0;
		int high = size;
		while (low < high) {
			int middle = (low + high) >>> 1;
			if(endZ[middle] < z) {
				low = middle + 1;
			} else {
				high = middle;
			}
		}

		for (int i = low; i < size && endZ[i] == z; i++) {
			Placement stackPlacement = placements[i];
			if(stackPlacement.getAbsoluteX() > maxX) {
				continue;
			}
			if(stackPlacement.getAbsoluteY() > maxY) {
				continue;
			}
			if(stackPlacement.getAbsoluteEndX() < minX) {
				continue;
			}
			if(stackPlacement.getAbsoluteEndY() < minY) {
				continue;
			}

			int x1 = Math.max(stackPlacement.getAbsoluteX(), minX);
			int y1 = Math.max(stackPlacement.getAbsoluteY(), minY);
			int x2 = Math.min(stackPlacement.getAbsoluteEndX(), maxX);
			int y2 = Math.min(stackPlacement.getAbsoluteEndY(), maxY);

			long intersect = (x2 - x1 + 1) * (y2 - y1 + 1);

			sum += intersect;

			if(sum == max) {
				break;
			}
		}
		return sum;
	}

	public boolean isFullSupport(List<Placement> stack, int minX, int minY, int minZ, BoxStackValue stackValue) {
		return stackValue.getArea() == calculateAreaSupport(stack, minX, minY, minZ, stackValue);
	}
}
