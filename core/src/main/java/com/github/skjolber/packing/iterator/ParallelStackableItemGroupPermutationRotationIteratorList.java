package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;

/**
 * 
 * This class is responsible for splitting the work load (as in the permutations) over multiple iterators.
 * 
 */

public class ParallelStackableItemGroupPermutationRotationIteratorList implements BoxItemGroupPermutationRotationIterator {

	protected final static int PADDING = 16;

	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder extends AbstractBoxItemGroupIteratorBuilder<Builder> {

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

			List<BoxItemGroup> groups = toMatrix();
			
			return new ParallelStackableItemGroupPermutationRotationIteratorList(groups, parallelizationCount);
		}
	}
	
	protected final int[] frequencies;
	protected ParallelStackableItemGroupPermutationRotationIterator[] workUnits;

	protected int workUnitIndex = 0;
	
	public ParallelStackableItemGroupPermutationRotationIteratorList(List<BoxItemGroup> groups, int parallelizationCount) {

		workUnits = new ParallelStackableItemGroupPermutationRotationIterator[parallelizationCount];
		for (int i = 0; i < parallelizationCount; i++) {

			// clone working variables so threads are less of the same
			// memory area as one another

			List<BoxItemGroup> clones = clone(groups);
			
			List<BoxItem> matrix = new ArrayList<>();
			for (BoxItemGroup loadableItemGroup : clones) {
				matrix.addAll(loadableItemGroup.getItems());
			}
			
			workUnits[i] = new ParallelStackableItemGroupPermutationRotationIterator(matrix.toArray(new BoxItem[matrix.size()]), clones);
			if(workUnits[i].preventOptmisation() != -1L) {
				throw new RuntimeException();
			}
		}

		this.frequencies = workUnits[0].calculateFrequencies();

		calculate();
	}
	
	private List<BoxItemGroup> clone(List<BoxItemGroup> groups) {
		List<BoxItemGroup> result = new ArrayList<>(groups.size());
		for (BoxItemGroup stackableItemGroup : groups) {
			result.add(stackableItemGroup.clone());
		}
		return result;
	}

	private void calculate() {
		int count = workUnits[0].getCount();
		List<BoxItemGroup> groups = workUnits[0].getGroups();

		if(count == 0) {
			return;
		}
		
		int[] reset = new int[PADDING + count];

		long permutationCount = workUnits[0].countPermutations();

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

	protected static int[] unrank(int[] frequencies, int elementCount, long permutationCount, long rank, List<BoxItemGroup> groups) {
	    int[] result = new int[PADDING + elementCount];
	    
	    int resultOffset = 0;
	    for (int j = 0; j < groups.size(); j++) {
	    	BoxItemGroup group = groups.get(j);
			
	    	int stackableItemsCount = group.stackableItemsCount();
	    	
		    for(int i = 0; i < stackableItemsCount; i++) {
		        for(int k = 0; k < group.size(); k++) {
		        	BoxItem item = (BoxItem)group.get(k);
		        	
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

	public int length() {
		return workUnits[workUnitIndex].length();
	}
	
	@Override
	public BoxStackValue getStackValue(int index) {
		return workUnits[workUnitIndex].getStackValue(index);
	}

	@Override
	public PermutationRotationState getState() {
		return workUnits[workUnitIndex].getState();
	}

	@Override
	public List<BoxStackValue> get(PermutationRotationState state, int length) {
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

	@Override
	public long countPermutations() {
		return workUnits[0].countPermutations();
	}

	@Override
	public long[] getMinStackableVolume() {
		return workUnits[workUnitIndex].getMinStackableVolume();
	}

	@Override
	public BoxItem[] getStackableItems() {
		return workUnits[workUnitIndex].getStackableItems();
	}

	@Override
	public List<BoxItemGroup> getGroups() {
		return workUnits[workUnitIndex].getGroups();
	}

}
