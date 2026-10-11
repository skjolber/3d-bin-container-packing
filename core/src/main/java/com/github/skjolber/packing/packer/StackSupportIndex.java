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
	 * (assuming placements in the stack do not overlap). The floor is not counted (returns 0 at {@code minZ == 0}): callers must treat
	 * {@code minZ == 0} as fully supported.
	 */
	public long calculateAreaSupport(List<Placement> stack, int minX, int minY, int minZ, BoxStackValue stackValue) {
		synchronize(stack);

		int maxX = minX + stackValue.getDx() - 1; // inclusive
		int maxY = minY + stackValue.getDy() - 1; // inclusive

		long max = stackValue.getArea();

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

		long sum = 0;
		for (int i = low; i < size && endZ[i] == z; i++) {
			sum += placements[i].overlapArea2D(minX, maxX, minY, maxY);
			if(sum == max) {
				break;
			}
		}
		return sum;
	}

	/**
	 * The floor is not counted: callers must treat {@code minZ == 0} as fully supported.
	 */
	public boolean isFullSupport(List<Placement> stack, int minX, int minY, int minZ, BoxStackValue stackValue) {
		return stackValue.getArea() == calculateAreaSupport(stack, minX, minY, minZ, stackValue);
	}
}
