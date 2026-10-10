package com.github.skjolber.packing.iterator;

import java.util.Arrays;
import java.util.List;

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
			if(parallelizationCount == -1) {
				throw new IllegalStateException();
			}
			
			AbstractBoxItemIteratorBuilder.BoxItemMatrix matrix = AbstractBoxItemIteratorBuilder.toMatrix(boxItems, dx, dy, dz, volume, maxLoadWeight);
			return new ParallelBoxItemPermutationRotationIteratorList(matrix.boxItems(), matrix.stackValues(), matrix.excluded(), parallelizationCount);
		}

	}	
	
	protected int parallelizationCount = -1;

	protected final int[] frequencies;
	/** The box items by index, shared with the work units' copies; not modified */
	private final BoxItem[] boxItems;
	/** The rotations of each box item which fit the container, by index; not modified */
	private final BoxStackValue[][] stackValues;
	/** The work units, each created when first requested (see {@link #getIterator(int)}) */
	protected final ParallelBoxItemPermutationRotationIterator[] workUnits;

	// the split of the remaining boxes, see calculate()
	private int count;
	private long permutationCount;
	/** The first rotations of the work units: all zero, shared (read-only) between the work units */
	private int[] reset;

	public ParallelBoxItemPermutationRotationIteratorList(BoxItem[] boxItems, BoxStackValue[][] stackValues, List<BoxItem> excluded, int parallelizationCount) {
		this.frequencies = new int[boxItems.length];

		for (int i = 0; i < boxItems.length; i++) {
			if(boxItems[i] != null) {
				frequencies[i] = boxItems[i].getCount();
			}
		}
		this.boxItems = boxItems;
		this.stackValues = stackValues;
		this.workUnits = new ParallelBoxItemPermutationRotationIterator[parallelizationCount];

		calculate();
	}

	private ParallelBoxItemPermutationRotationIteratorList(ParallelBoxItemPermutationRotationIteratorList source) {
		this.parallelizationCount = source.parallelizationCount;
		this.frequencies = source.frequencies.clone();
		this.boxItems = source.boxItems;
		this.stackValues = source.stackValues;
		// the work units are created from the frequencies (they always start at their first permutation)
		this.workUnits = new ParallelBoxItemPermutationRotationIterator[source.workUnits.length];
		this.count = source.count;
		this.permutationCount = source.permutationCount;
		this.reset = source.reset;
	}

	public ParallelBoxItemPermutationRotationIteratorList fork() {
		return new ParallelBoxItemPermutationRotationIteratorList(this);
	}

	private BoxItem[] copy(BoxItem[] boxItems) {
		BoxItem[] result = new BoxItem[boxItems.length];
		for(int i = 0; i < boxItems.length; i++) {
			
			BoxItem boxItem = boxItems[i];
			if(boxItem != null) {
				result[i] = new BoxItem(boxItem.getBox(), boxItem.getCount(), i, boxItem.getGlobalIndex()).withOrderingOf(boxItem);
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

	/**
	 * Back to the first permutations and rotations of the work units, see {@link BoxItemPermutationRotationIterator#reset()}.
	 */
	public void reset() {
		for (ParallelBoxItemPermutationRotationIterator workUnit : workUnits) {
			if(workUnit != null) {
				workUnit.reset();
			}
		}
	}

	/**
	 * Split the remaining boxes between the work units: the work units are discarded, and created again (at their first
	 * permutation) when requested. This is the only part which is proportional to the box count and not to the work
	 * unit count.
	 */
	private void calculate() {
		Arrays.fill(workUnits, null);

		count = getCount();
		if(count == 0) {
			return;
		}

		reset = new int[count];

		int first = firstDuplicate(frequencies);
		if(first == -1) {
			permutationCount = getPermutationCount(count);
		} else {
			permutationCount = getPermutationCountWithRepeatedItems(count, first);
		}

		if(permutationCount == -1L) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Create a work unit: it starts at the first permutation of its share of the permutations, and ends at the first
	 * permutation of the next work unit.
	 * <br>
	 * This only reads the state of the list, so different work units can be created from different threads.
	 */
	private ParallelBoxItemPermutationRotationIterator createWorkUnit(int index) {
		// copy working variables so threads are less of the same
		// memory area as one another
		ParallelBoxItemPermutationRotationIterator workUnit = new ParallelBoxItemPermutationRotationIterator(copy(boxItems), stackValues, this);
		if(count == 0) {
			return workUnit;
		}

		workUnit.setPermutations(firstPermutation(index));
		workUnit.setRotations(new int[reset.length]);
		workUnit.setReset(reset);
		workUnit.calculateMinStackableVolume(0);

		if(index < workUnits.length - 1) {
			workUnit.setLastPermutation(firstPermutation(index + 1));
		}
		return workUnit;
	}

	private int[] firstPermutation(int index) {
		long rank = (permutationCount * index) / workUnits.length;

		rank++;

		// use more complex n-th lexographical permutation algorithm
		// which also handles zero frequencies
		return kthPermutation(frequencies.clone(), count, permutationCount, rank);
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

	/**
	 * @return all work units, creating those which do not exist yet
	 */
	public ParallelBoxItemPermutationRotationIterator[] getIterators() {
		for (int i = 0; i < workUnits.length; i++) {
			getIterator(i);
		}
		return workUnits;
	}

	/**
	 * Get a work unit, created when first requested. Different threads can request different work units, as long as the
	 * remaining boxes are not changed ({@linkplain #removePermutations(List)}) concurrently.
	 *
	 * @param i work unit index
	 * @return the work unit
	 */
	public ParallelBoxItemPermutationRotationIterator getIterator(int i) {
		ParallelBoxItemPermutationRotationIterator workUnit = workUnits[i];
		if(workUnit == null) {
			workUnit = createWorkUnit(i);
			workUnits[i] = workUnit;
		}
		return workUnit;
	}
}
