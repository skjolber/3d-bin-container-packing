package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;

public abstract class AbstractBoxItemPermutationRotationIterator implements BoxItemPermutationRotationIterator {

	protected final RemainingBoxItem[] stackableItems; // by index
	/** The rotations of each box item which fit the container (stack values of its box), by index; not modified */
	protected final BoxStackValue[][] stackValues;
	protected int[] rotations;
	protected int[] reset;

	// permutations of boxes that fit inside this container
	protected int[] permutations; // n!

	// minimum volume from index i and above
	protected long[] minBoxVolume;
	
	public AbstractBoxItemPermutationRotationIterator(RemainingBoxItem[] matrix, BoxStackValue[][] stackValues) {
		this.stackableItems = matrix;
		this.stackValues = stackValues;
	}

	/**
	 * @return the rotations of a box which fit inside a load size (stack values of the box), or null if none
	 */
	public static BoxStackValue[] getRotations(Box box, int dx, int dy, int dz) {
		List<BoxStackValue> rotations = box.rotations(dx, dy, dz);
		if(rotations == null || rotations.isEmpty()) {
			return null;
		}
		return rotations.toArray(new BoxStackValue[rotations.size()]);
	}

	protected static RemainingBoxItem[] copyBoxItems(RemainingBoxItem[] source) {
		RemainingBoxItem[] copy = new RemainingBoxItem[source.length];
		for(int i = 0; i < source.length; i++) {
			if(source[i] != null) {
				copy[i] = source[i].copy();
			}
		}
		return copy;
	}

	/**
	 * Get number of box items within the constraints.
	 *
	 * @return number between 0 and number of {@linkplain RemainingBoxItem}s used in the constructor.
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
			results.add(stackValues[permutations[i]][rotations[i]]);
		}
		return results;
	}

	public abstract int length();

	@Override
	public void reset() {
		// the first permutation is in ascending order
		Arrays.sort(permutations);
		System.arraycopy(reset, 0, rotations, 0, rotations.length);
		if(permutations.length > 0) {
			calculateMinStackableVolume(0);
		}
	}

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
			int rotationCount = stackValues[permutations[i]].length;
			if(Long.MAX_VALUE / rotationCount <= n) {
				return -1L;
			}

			n = n * rotationCount;
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
		for (RemainingBoxItem value : stackableItems) {
			if(value != null) {
				if(maxCount < value.getCount()) {
					maxCount = value.getCount();
				}
			}
		}

		long n = 1;
		if(maxCount > 1) {
			int[] factors = new int[maxCount];
			for (RemainingBoxItem value : stackableItems) {
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
	
	public RemainingBoxItem[] getBoxItems() {
		return stackableItems;
	}
	public long getMinBoxVolume(int offset) {
		return minBoxVolume[offset];
	}
	
	public long[] getMinBoxVolume() {
		return minBoxVolume;
	}
	
	protected void calculateMinStackableVolume(int offset) {
		BoxStackValue last = stackValues[permutations[permutations.length - 1]][rotations[permutations.length - 1]];

		minBoxVolume[permutations.length - 1] = last.getVolume();

		for (int i = permutations.length - 2; i >= offset; i--) {
			long volume = stackValues[permutations[i]][rotations[i]].getVolume();

			if(volume < minBoxVolume[i + 1]) {
				minBoxVolume[i] = volume;
			} else {
				minBoxVolume[i] = minBoxVolume[i + 1];
			}
		}
	}
	
	@Override
	public BoxStackValue getStackValue(int index) {
		return stackValues[permutations[index]][rotations[index]];
	}

	@Override
	public BoxStackValue[] getStackValues(int index) {
		return stackValues[permutations[index]];
	}

	@Override
	public BoxStackValue[][] getBoxItemStackValues() {
		return stackValues;
	}

	@Override
	public RemainingBoxItem getBoxItem(int index) {
		return stackableItems[permutations[index]];
	}


	
}
