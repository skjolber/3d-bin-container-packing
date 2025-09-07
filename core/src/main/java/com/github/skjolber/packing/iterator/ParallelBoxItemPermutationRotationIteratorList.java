package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;

/**
 * 
 * This class is responsible for splitting the work load (as in the permutations) over multiple iterators.
 * 
 */

public class ParallelBoxItemPermutationRotationIteratorList {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		protected int maxLoadWeight = -1;
		protected int dx = -1;
		protected int dy = -1;
		protected int dz = -1;
		protected long volume = -1L;

		protected List<BoxItem> boxItems;

		protected int parallelizationCount = -1;

		public Builder withLoadSize(int dx, int dy, int dz) {
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			
			this.volume = (long)dx * (long)dy * (long)dz;
			return this;
		}

		public Builder withMaxLoadWeight(int maxLoadWeight) {
			this.maxLoadWeight = maxLoadWeight;

			return this;
		}

		public Builder withBoxItems(List<BoxItem> stackableItems) {
			this.boxItems = stackableItems;

			return this;
		}
		
		public Builder withParallelizationCount(int parallelizationCount) {
			this.parallelizationCount = parallelizationCount;
			return this;
		}
				
		public ParallelBoxItemPermutationRotationIteratorList build() {
			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(dx == -1 || dy == -1 || dz == -1) {
				throw new IllegalStateException();
			}

			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(parallelizationCount == -1) {
				throw new IllegalStateException();
			}
			
			BoxItem[] included = new BoxItem[boxItems.size()];
			List<BoxItem> excluded = new ArrayList<>(boxItems.size());
			
			// box item and box item groups indexes are unique and static
			for (int i = 0; i < boxItems.size(); i++) {
				BoxItem boxItem = boxItems.get(i);
				
				Box box = boxItem.getBox();
				if(box.getWeight() > maxLoadWeight) {
					excluded.add(boxItem);
					continue;
				}

				if(box.getVolume() > volume) {
					excluded.add(boxItem);
					continue;
				}
				
				List<BoxStackValue> boundRotations = box.rotations(dx, dy, dz);
				if(boundRotations == null || boundRotations.isEmpty()) {
					excluded.add(boxItem);
					continue;
				}
				
				List<BoxStackValue> cloned = new ArrayList<>(boundRotations.size());
				for(BoxStackValue v : boundRotations) {
					cloned.add(v.clone());
				}
				Box clonedBox = new Box(box, cloned);
				
				included[i] = new BoxItem(clonedBox, boxItem.getCount(), i);
			}

			return new ParallelBoxItemPermutationRotationIteratorList(included, excluded, parallelizationCount);
		}

	}	
	
	protected int parallelizationCount = -1;
	
	protected final int[] frequencies;
	protected ParallelBoxItemPermutationRotationIterator[] workUnits;

	public ParallelBoxItemPermutationRotationIteratorList(BoxItem[] boxItems, List<BoxItem> excluded, int parallelizationCount) {
		this.frequencies = new int[boxItems.length];

		for (int i = 0; i < boxItems.length; i++) {
			if(boxItems[i] != null) {
				frequencies[i] = boxItems[i].getCount();
			}
		}

		workUnits = new ParallelBoxItemPermutationRotationIterator[parallelizationCount];
		for (int i = 0; i < parallelizationCount; i++) {
			
			// clone working variables so threads are less of the same
			// memory area as one another
			BoxItem[] clone = clone(boxItems);
			workUnits[i] = new ParallelBoxItemPermutationRotationIterator(clone, this);
		}

		calculate();
	}

	private BoxItem[] clone(BoxItem[] boxItems) {
		BoxItem[] result = new BoxItem[boxItems.length];
		for(int i = 0; i < boxItems.length; i++) {
			
			BoxItem boxItem = boxItems[i];
			if(boxItem != null) {
				Box box = boxItem.getBox();
				
				List<BoxStackValue> cloned = new ArrayList<>(boxItems.length);
				for(BoxStackValue v : box.getStackValues()) {
					cloned.add(v.clone());
				}
				Box clonedBox = new Box(box, cloned);
				
				result[i] = new BoxItem(clonedBox, boxItem.getCount(), i);
			}
		}
		return result;
	}

	public void removePermutations(List<Integer> removed) {
		for (Integer integer : removed) {
			if(frequencies[integer] > 0) {
				frequencies[integer]--;
			}
		}

		calculate();
	}

	private void calculate() {
		int count = getCount();

		if(count == 0) {
			return;
		}
		
		int[] reset = new int[count];

		long countPermutations;
		int first = firstDuplicate(frequencies);
		if(first == -1) {
			countPermutations = getPermutationCount(count);
		} else {
			countPermutations = getPermutationCountWithRepeatedItems(count, first);
		}

		if(countPermutations == -1L) {
			throw new IllegalArgumentException();
		}

		int[] copyOfFrequencies = new int[frequencies.length];
		for (int i = 0; i < workUnits.length; i++) {
			long rank = (countPermutations * i) / workUnits.length;

			rank++;

			// use more complex n-th lexographical permutation algorithm
			// which also handles zero frequencies
			System.arraycopy(frequencies, 0, copyOfFrequencies, 0, frequencies.length);
			int[] permutations = kthPermutation(copyOfFrequencies, count, countPermutations, rank);

			workUnits[i].setPermutations(permutations);
			workUnits[i].setRotations(new int[reset.length]);
			workUnits[i].setReset(reset);
			workUnits[i].calculateMinStackableVolume(0);
		}

		for (int i = 0; i < workUnits.length - 1; i++) {
			int[] nextWorkUnitPermutations = workUnits[i + 1].getPermutations();
			int[] lexiographicalLimit = new int[nextWorkUnitPermutations.length];

			System.arraycopy(nextWorkUnitPermutations, 0, lexiographicalLimit, 0, lexiographicalLimit.length);

			workUnits[i].setLastPermutation(lexiographicalLimit);
		}
	}

	private int getCount() {
		int count = 0;
		for (int f : frequencies) {
			count += f;
		}
		return count;
	}

	public long countPermutations() {
		return countPermutations(getCount());
	}

	long countPermutations(int count) {
		int first = firstDuplicate(frequencies);
		if(first == -1) {
			return getPermutationCount(count);
		} else {
			return getPermutationCountWithRepeatedItems(count, first);
		}
	}

	private long getPermutationCount(int count) {
		long permutationCount = 1;
		for (int i = 0; i < count; i++) {
			if(Long.MAX_VALUE / (i + 1) <= permutationCount) {
				return -1L;
			}
			permutationCount = permutationCount * (i + 1);
		}
		return permutationCount;
	}

	private long getPermutationCountWithRepeatedItems(int count, int first) {
		long permutationCount = 1;
		// cancel out the first set of factors
		// 
		// For [3, 4] this would look like:
		//
		// 1 * 2 * 3 * 4 * 5 * 6 * 7
		// -----------------------------
		// (1 * 2 * 3) (1 * 2 * 3 * 4)
		//
		// which is equal to
		//
		// 4 * 5 * 6 * 7
		// -----------------------------
		// (1 * 2 * 3 * 4)
		//
		// above the line:
		for (int i = frequencies[first]; i < count; i++) {
			if(Long.MAX_VALUE / (i + 1) <= permutationCount) {
				return -1L;
			}
			permutationCount = permutationCount * (i + 1);
		}
		// below the line:
		for (int i = first + 1; i < frequencies.length; i++) {
			if(frequencies[i] > 1) {
				for (int k = 1; k < frequencies[i]; k++) {
					permutationCount = permutationCount / (k + 1);
				}
			}
		}
		// future improvement: cancel out more
		return permutationCount;
	}

	private static int firstDuplicate(int[] frequencies) {
		for (int i = 0; i < frequencies.length; i++) {
			if(frequencies[i] > 1) {
				return i;
			}
		}
		return -1;
	}

	// https://stemhash.com/efficient-permutations-in-lexicographic-order/
	static int[] kthPermutation(int[] frequencies, int elementCount, long permutationCount, long rank) {
		int[] result = new int[elementCount];

		for (int i = 0; i < elementCount; i++) {
			for (int k = 0; k < frequencies.length; k++) {
				if(frequencies[k] == 0) {
					continue;
				}
				long suffixcount = permutationCount * frequencies[k] / (elementCount - i);
				if(rank <= suffixcount) {
					result[i] = k;

					permutationCount = suffixcount;

					frequencies[k]--;
					break;
				}
				rank -= suffixcount;
			}
		}
		return result;
	}

	public ParallelBoxItemPermutationRotationIterator[] getIterators() {
		return workUnits;
	}

	public ParallelBoxItemPermutationRotationIterator getIterator(int i) {
		return workUnits[i];
	}
}
