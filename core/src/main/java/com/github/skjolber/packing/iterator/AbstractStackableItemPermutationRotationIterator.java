package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;

public abstract class AbstractStackableItemPermutationRotationIterator implements BoxItemPermutationRotationIterator {

	protected final IndexedStackableItem[] stackableItems; // by index
	
	protected int[] rotations; // 2^n or 6^n
	protected int[] reset;

	// permutations of boxes that fit inside this container
	protected int[] permutations; // n!

	// minimum volume from index i and above
	protected long[] minStackableVolume;
	
	public AbstractStackableItemPermutationRotationIterator(IndexedStackableItem[] matrix) {
		this.stackableItems = matrix;
	}
	
	/**
	 * Get number of box items within the constraints.
	 *
	 * @return number between 0 and number of {@linkplain BoxItem}s used in the constructor.
	 */

	public int boxItemLength() {
		return stackableItems.length;
	}

	public long getMinStackableArea(int offset) {
		long minArea = Long.MAX_VALUE;
		for (int i = offset; i < length(); i++) {
			BoxStackValue permutationRotation = getStackValue(i);
			long area = permutationRotation.getArea();
			if(area < minArea) {
				minArea = area;
			}
		}
		return minArea;
	}

	public int getMinStackableAreaIndex(int offset) {
		long minArea = getStackValue(offset).getArea();
		int index = offset;

		for (int i = offset + 1; i < length(); i++) {
			BoxStackValue permutationRotation = getStackValue(i);
			long area = permutationRotation.getArea();
			if(area < minArea) {
				minArea = area;
				index = i;
			}
		}
		return index;
	}

	public List<BoxStackValue> get(PermutationRotationState state, int length) {
		int[] permutations = state.getPermutations();
		int[] rotations = state.getRotations();

		List<BoxStackValue> results = new ArrayList<>(length);
		for (int i = 0; i < length; i++) {
			results.add(stackableItems[permutations[i]].getStackable().getStackValue(rotations[i]));
		}
		return results;
	}

	public abstract int length();

	protected int[] calculateFrequencies() {
		int[] frequencies = new int[stackableItems.length];
		
		for (int i = 0; i < stackableItems.length; i++) {
			if(stackableItems[i] != null) {
				frequencies[i] = stackableItems[i].getCount();
			}
		}
		return frequencies;
	}
	

	public long countRotations() {
		int[] permutations = getPermutations();
		
		long n = 1;
		for (int i = 0; i < permutations.length; i++) {
			IndexedStackableItem value = stackableItems[permutations[i]];
			if(Long.MAX_VALUE / value.getStackable().getStackValues().length <= n) {
				return -1L;
			}

			n = n * value.getStackable().getStackValues().length;
		}
		return n;
	}

	/**
	 * Return number of permutations for boxes which fit within this container.
	 * 
	 * @return permutation count
	 */

	public long countPermutations() {
		// reduce permutations for boxes which are duplicated

		// could be further bounded by looking at how many boxes (i.e. n x the smallest) which actually
		// fit within the container volume

		int[] permutations = getPermutations();
		
		int maxCount = 0;
		for (IndexedStackableItem value : stackableItems) {
			if(value != null) {
				if(maxCount < value.getCount()) {
					maxCount = value.getCount();
				}
			}
		}

		long n = 1;
		if(maxCount > 1) {
			int[] factors = new int[maxCount];
			for (IndexedStackableItem value : stackableItems) {
				if(value != null) {
					for (int k = 0; k < value.getCount(); k++) {
						factors[k]++;
					}
				}
			}

			for (long i = 0; i < permutations.length; i++) {
				if(Long.MAX_VALUE / (i + 1) <= n) {
					return -1L;
				}

				n = n * (i + 1);

				for (int k = 1; k < maxCount; k++) {
					while (factors[k] > 0 && n % (k + 1) == 0) {
						n = n / (k + 1);

						factors[k]--;
					}
				}
			}

			for (int k = 1; k < maxCount; k++) {
				while (factors[k] > 0) {
					n = n / (k + 1);

					factors[k]--;
				}
			}
		} else {
			for (long i = 0; i < permutations.length; i++) {
				if(Long.MAX_VALUE / (i + 1) <= n) {
					return -1L;
				}
				n = n * (i + 1);
			}
		}
		return n;
	}
	
	public IndexedStackableItem[] getStackableItems() {
		return stackableItems;
	}
	public long getMinStackableVolume(int offset) {
		return minStackableVolume[offset];
	}
	
	public long[] getMinStackableVolume() {
		return minStackableVolume;
	}
	
	protected void calculateMinStackableVolume(int offset) {
		BoxStackValue last = stackableItems[permutations[permutations.length - 1]].getStackable().getStackValue(rotations[permutations.length - 1]);

		minStackableVolume[permutations.length - 1] = last.getVolume();

		for (int i = permutations.length - 2; i >= offset; i--) {
			long volume = stackableItems[permutations[i]].getStackable().getStackValue(rotations[i]).getVolume();

			if(volume < minStackableVolume[i + 1]) {
				minStackableVolume[i] = volume;
			} else {
				minStackableVolume[i] = minStackableVolume[i + 1];
			}
		}
	}
	
	@Override
	public BoxStackValue getStackValue(int index) {
		return stackableItems[permutations[index]].getStackable().getStackValue(rotations[index]);
	}


	
}
