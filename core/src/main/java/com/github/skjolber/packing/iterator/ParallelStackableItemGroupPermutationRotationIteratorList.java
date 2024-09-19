package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.StackableItem;

/**
 * 
 * This class is responsible for splitting the work load (as in the permutations) over multiple iterators.
 * 
 */

public class ParallelStackableItemGroupPermutationRotationIteratorList extends AbstractStackableItemGroupPermutationRotationIterator {

	protected final static int PADDING = 16;

	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder extends AbstractStackableItemGroupIteratorBuilder<Builder> {

		private int parallelizationCount = -1;

		public Builder withParallelizationCount(int parallelizationCount) {
			this.parallelizationCount = parallelizationCount;

			return this;
		}
		
		public ParallelStackableItemGroupPermutationRotationIteratorList build() {
			if(parallelizationCount == -1) {
				throw new IllegalStateException();
			}
			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(size == null) {
				throw new IllegalStateException();
			}

			List<IndexedStackableItemGroup> groups = toMatrix();
			
			List<StackableItem> matrix = new ArrayList<>();
			for (IndexedStackableItemGroup loadableItemGroup : groups) {
				matrix.addAll(loadableItemGroup.getItems());
			}
			
			return new ParallelStackableItemGroupPermutationRotationIteratorList(matrix.toArray(new IndexedStackableItem[matrix.size()]), groups, parallelizationCount);
		}

	}
	
	protected final int[] frequencies;
	protected ParallelStackableItemGroupPermutationRotationIterator[] workUnits;

	protected int workUnitIndex = 0;
	
	public ParallelStackableItemGroupPermutationRotationIteratorList(IndexedStackableItem[] matrix, List<IndexedStackableItemGroup> groups, int parallelizationCount) {
		super(matrix, groups);
		
		this.frequencies = new int[matrix.length];
		for (int i = 0; i < matrix.length; i++) {
			if(matrix[i] != null) {
				frequencies[i] = matrix[i].getCount();
			}
		}
		

		workUnits = new ParallelStackableItemGroupPermutationRotationIterator[parallelizationCount];
		for (int i = 0; i < parallelizationCount; i++) {
			
			// clone working variables so threads are less of the same
			// memory area as one another
			IndexedStackableItem[] clone = clone(matrix);
			
			workUnits[i] = new ParallelStackableItemGroupPermutationRotationIterator(clone, clone(groups));
			if(workUnits[i].preventOptmisation() != -1L) {
				throw new RuntimeException();
			}
		}

		calculate();
	}
	
	private List<IndexedStackableItemGroup> clone(List<IndexedStackableItemGroup> groups) {
		List<IndexedStackableItemGroup> result = new ArrayList<>();
		for (IndexedStackableItemGroup stackableItemGroup : groups) {
			result.add(stackableItemGroup.clone());
		}
		return result;
	}

	private IndexedStackableItem[] clone(IndexedStackableItem[] matrix) {
		IndexedStackableItem[] clone = new IndexedStackableItem[matrix.length];
		for(int i = 0; i < clone.length; i++) {
			IndexedStackableItem item = matrix[i];
			if(item != null) {
				clone[i] = item.clone();
			}
		}
		return clone;
	}

	private void calculate() {
		int count = getCount();

		if(count == 0) {
			return;
		}
		
		int[] reset = new int[PADDING + count];

		long permutationCount = countPermutations();

		if(permutationCount == -1L) {
			throw new IllegalArgumentException();
		}

		int[] copyOfFrequencies = new int[frequencies.length];
		for (int i = 0; i < workUnits.length; i++) {
			long permutationNumber = (permutationCount * i) / workUnits.length;

			permutationNumber++;

			System.arraycopy(frequencies, 0, copyOfFrequencies, 0, frequencies.length);
			int[] permutations = unrank(copyOfFrequencies, count, permutationCount, permutationNumber, groups);
			
			workUnits[i].setPermutations(permutations);
			workUnits[i].setRotations(new int[reset.length]);
			workUnits[i].setReset(reset);
			workUnits[i].initMinStackableVolume();
		}

		for (int i = 0; i < workUnits.length - 1; i++) {
			int[] nextWorkUnitPermutations = workUnits[i + 1].getPermutations();
			int[] lexiographicalLimit = new int[PADDING + nextWorkUnitPermutations.length];

			System.arraycopy(nextWorkUnitPermutations, 0, lexiographicalLimit, PADDING, nextWorkUnitPermutations.length);

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
		int first = firstDuplicate(frequencies);
		if(first == -1) {
			return getPermutationCount();
		} else {
			return getPermutationCountWithRepeatedItems();
		}
	}

	private long getPermutationCount() {
		long n = 1;
		
		for (IndexedStackableItemGroup loadableItemGroup : groups) {
			int count = loadableItemGroup.stackableItemsCount();
			
			for (long i = 0; i < count; i++) {
				if(Long.MAX_VALUE / (i + 1) <= n) {
					return -1L;
				}
				n = n * (i + 1);
			}
		}
		return n;
	}

	/**
	 * Return number of permutations for boxes which fit within this container.
	 * 
	 * @return permutation count
	 */

	public long getPermutationCountWithRepeatedItems() {
		// reduce permutations for boxes which are duplicated

		// could be further bounded by looking at how many boxes (i.e. n x the smallest) which actually
		// fit within the container volume
		long n = 1;

		for (IndexedStackableItemGroup loadableItemGroup : groups) {

			List<IndexedStackableItem> items = loadableItemGroup.getItems();
			
			int count = loadableItemGroup.stackableItemsCount();
			
			int maxCount = 0;
			for (StackableItem value : items) {
				if(value != null) {
					if(maxCount < value.getCount()) {
						maxCount = value.getCount();
					}
				}
			}
	
			if(maxCount > 1) {
				int[] factors = new int[maxCount];
				for (StackableItem value : items) {
					if(value != null) {
						for (int k = 0; k < value.getCount(); k++) {
							factors[k]++;
						}
					}
				}
	
				for (long i = 0; i < count; i++) {
					if(Long.MAX_VALUE / (i + 1) <= n) {
						return -1L;
					}
	
					n = n * (i + 1);
	
					// reduce n if possible
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
				for (long i = 0; i < count; i++) {
					if(Long.MAX_VALUE / (i + 1) <= n) {
						return -1L;
					}
					n = n * (i + 1);
				}
			}
		}
		return n;
	}
	
	private static int firstDuplicate(int[] frequencies) {
		for (int i = 0; i < frequencies.length; i++) {
			if(frequencies[i] > 1) {
				return i;
			}
		}
		return -1;
	}

	protected static int[] unrank(int[] frequencies, int elementCount, long permutationCount, long rank, List<IndexedStackableItemGroup> groups) {
	    int[] result = new int[PADDING + elementCount];
	    
	    int resultOffset = 0;
	    for (int j = 0; j < groups.size(); j++) {
	    	IndexedStackableItemGroup group = groups.get(j);
			
	    	int stackableItemsCount = group.stackableItemsCount();
	    	
		    for(int i = 0; i < stackableItemsCount; i++) {
		        for(int k = 0; k < group.size(); k++) {
		        	IndexedStackableItem item = (IndexedStackableItem)group.get(k);
		        	
		        	int index = item.getIndex();
		        	
		            if(frequencies[index] == 0) {
		                continue;
		            }
		            // suffixcount is the number of distinct perms that begin with x
		            long suffixcount = permutationCount * frequencies[index] / (stackableItemsCount - i);
		            if (rank <= suffixcount) {
		                result[PADDING + resultOffset + i] = index;
	
		                permutationCount = suffixcount;
	
		                frequencies[index]--;
		                break;
		            }
		            rank -= suffixcount;
		        }
		    }
		    
		    resultOffset += stackableItemsCount;
	    }
	    return result;
	}


	public ParallelStackableItemGroupPermutationRotationIterator[] getIterators() {
		return workUnits;
	}

	public ParallelStackableItemGroupPermutationRotationIterator getIterator(int i) {
		return workUnits[i];
	}
	
	public int[] getFrequencies() {
		return frequencies;
	}

	public int length() {
		return workUnits[workUnitIndex].length();
	}
	
	@Override
	public StackValue getStackValue(int index) {
		return workUnits[workUnitIndex].getStackValue(index);
	}

	@Override
	public PermutationRotationState getState() {
		return workUnits[workUnitIndex].getState();
	}

	@Override
	public List<StackValue> get(PermutationRotationState state, int length) {
		return workUnits[workUnitIndex].get(state, length);
	}

	@Override
	public long getMinStackableVolume(int index) {
		return workUnits[workUnitIndex].getMinStackableVolume(index);
	}

	@Override
	public int getMinStackableAreaIndex(int index) {
		return workUnits[workUnitIndex].getMinStackableAreaIndex(index);
	}

	@Override
	public int[] getPermutations() {
		return workUnits[workUnitIndex].getPermutations();
	}

	@Override
	public long countRotations() {
		return workUnits[workUnitIndex].countRotations();
	}

	@Override
	public int nextRotation() {
		return workUnits[workUnitIndex].nextRotation();
	}

	@Override
	public int nextRotation(int maxIndex) {
		return workUnits[workUnitIndex].nextRotation(maxIndex);
	}

	@Override
	public int nextPermutation() {
		while(workUnitIndex < workUnits.length) {
			int nextPermutation = workUnits[workUnitIndex].nextPermutation();
			
			if(nextPermutation != -1) {
				return nextPermutation;
			}

			// compare previous permutation to the next
			workUnitIndex++;
			if(workUnitIndex < workUnits.length) {
				int[] permutations = workUnits[workUnitIndex].getPermutations();

				// TODO how to find the correct index here?
				for(int i = permutations.length - 2; i >= 0; i--) {
					if(permutations[i] > permutations[i + 1]) {
						return i;
					}
				}
				
				// should never happen
				return 0;
			}
		}
		return -1;
	}

	@Override
	public int nextPermutation(int maxIndex) {
		
		iterators:
		while(workUnitIndex < workUnits.length) {
			int nextPermutation = workUnits[workUnitIndex].nextPermutation(maxIndex);
			
			if(nextPermutation != -1) {
				return nextPermutation;
			}
			// compare previous permutation to the next
			workUnitIndex++;
			if(workUnitIndex < workUnits.length) {
				int[] permutations = workUnits[workUnitIndex].getPermutations();

				// TODO how to find the correct index here?
				for(int i = permutations.length - 2; i >= 0; i--) {
					if(permutations[i] > permutations[i + 1]) {
						if(i <= maxIndex) {
							return i;
						} else {
							continue iterators;
						}
					}
				}
				
				// should never happen
				return 0;
			}
		}
		return -1;
	}

	@Override
	public void removePermutations(int count) {
		int[] permutations = workUnits[workUnitIndex].getPermutations();
		
		List<Integer> removed = new ArrayList<>(permutations.length);
		
		for(int i = 0; i < count; i++) {
			removed.add(permutations[i]);
		}
		
		removePermutations(removed);
	}
	
	public void removePermutations(List<Integer> removed) {
		super.removePermutations(removed);
		
		for (Integer integer : removed) {
			if(frequencies[integer] > 0) {
				frequencies[integer]--;
			}
		}

		for (ParallelStackableItemGroupPermutationRotationIterator unit : workUnits) {
			unit.removePermutations(removed);
		}
		
		calculate();
	}

}
