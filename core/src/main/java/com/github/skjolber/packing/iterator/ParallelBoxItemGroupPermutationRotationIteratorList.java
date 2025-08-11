package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;

/**
 * 
 * This class is responsible for splitting the work load (as in the permutations) over multiple iterators.
 * 
 */

public class ParallelBoxItemGroupPermutationRotationIteratorList implements BoxItemGroupPermutationRotationIterator {

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
		
		public ParallelBoxItemGroupPermutationRotationIteratorList build() {
			if(parallelizationCount == -1) {
				throw new IllegalStateException();
			}
			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(size == null) {
				throw new IllegalStateException();
			}

			List<BoxItemGroup> included = new ArrayList<>(boxItemGroups.size());
			List<BoxItemGroup> excluded = new ArrayList<>(boxItemGroups.size());
			
			// box item and box item groups indexes are unique and static
			
			int offset = 0;
			for (int i = 0; i < boxItemGroups.size(); i++) {
				BoxItemGroup group = boxItemGroups.get(i);
				if(fitsInside(group)) {
					List<BoxItem> loadableItems = new ArrayList<>(group.size());
					for (int k = 0; k < group.size(); k++) {
						BoxItem item = group.get(k);
	
						Box box = item.getBox();
						
						List<BoxStackValue> boundRotations = box.rotations(size);
						Box boxClone = new Box(box, boundRotations);
						
						loadableItems.add(new BoxItem(boxClone, item.getCount(), offset));
						
						offset++;
					}
					included.add(new BoxItemGroup(group.getId(), loadableItems, i));
				} else {
					excluded.add(group);
					
					offset += group.size();
				}
			}

			BoxItemGroup[] groupIndex = new BoxItemGroup[boxItemGroups.size()];
			BoxItem[] boxIndex = new BoxItem[offset];
			
			for (BoxItemGroup loadableItemGroup : included) {
				groupIndex[loadableItemGroup.getIndex()] = loadableItemGroup;
				for (int k = 0; k < loadableItemGroup.size(); k++) {
					BoxItem item = loadableItemGroup.get(k);
					boxIndex[item.getIndex()] = item;
				}
			}
			
			return new ParallelBoxItemGroupPermutationRotationIteratorList(groupIndex, boxIndex, excluded, parallelizationCount);
		}
	}
	
	protected final int[] frequencies;
	protected ParallelBoxItemGroupPermutationRotationIterator[] workUnits;

	protected int workUnitIndex = 0;
	
	protected BoxItemGroup[] groupsMatrix;
	protected BoxItem[] boxMatrix;
	protected List<BoxItemGroup> excluded;
	
	public ParallelBoxItemGroupPermutationRotationIteratorList(BoxItemGroup[] boxItemGroups, BoxItem[] boxItems, List<BoxItemGroup> excluded, int parallelizationCount) {
		workUnits = new ParallelBoxItemGroupPermutationRotationIterator[parallelizationCount];
		for (int i = 0; i < parallelizationCount; i++) {

			// clone working variables so threads are less of the same
			// memory area as one another
			BoxItem[] boxMatrixClone = new BoxItem[boxItems.length];			

			BoxItemGroup[] groupsMatrixClone = new BoxItemGroup[boxItemGroups.length];
			for(int k = 0; k < groupsMatrixClone.length; k++) {
				groupsMatrixClone[k] = boxItemGroups[k].clone();
				
				for(int l = 0; l < groupsMatrixClone[k].size(); l++) {
					BoxItem item =  groupsMatrixClone[k].get(l);
					boxMatrixClone[item.getIndex()] = item;
				}
			}
			
			workUnits[i] = new ParallelBoxItemGroupPermutationRotationIterator(groupsMatrixClone, boxMatrixClone, excluded);
			if(workUnits[i].preventOptmisation() != -1L) {
				throw new RuntimeException();
			}
		}
		
		this.excluded = excluded;
		this.groupsMatrix = boxItemGroups;
		this.boxMatrix = boxItems;
		
		this.frequencies = workUnits[0].calculateFrequencies();

		calculate();
	}
	
	private void calculate() {
		int count = workUnits[0].getBoxCount();
		BoxItemGroup[] groups = workUnits[0].getBoxItemGroups();

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

	protected static int[] unrank(int[] frequencies, int elementCount, long permutationCount, long rank,  BoxItemGroup[] groups) {
	    int[] result = new int[PADDING + elementCount];
	    
	    int resultOffset = 0;
	    for (int j = 0; j < groups.length; j++) {
	    	
	    	BoxItemGroup group = groups[j];
	    	if(group == null) {
	    		continue;
	    	}
	    	int groupBoxCount = group.getBoxCount();
	    	
		    for(int i = 0; i < groupBoxCount; i++) {
		        for(int k = 0; k < group.size(); k++) {
		        	BoxItem item = (BoxItem)group.get(k);
		        	
		        	int index = item.getIndex();
		        	
		            if(frequencies[index] == 0) {
		                continue;
		            }
		            // suffixcount is the number of distinct perms that begin with x
		            long suffixcount = permutationCount * frequencies[index] / (groupBoxCount - i);
		            if (rank <= suffixcount) {
		                result[PADDING + resultOffset + i] = index;
	
		                permutationCount = suffixcount;
	
		                frequencies[index]--;
		                break;
		            }
		            rank -= suffixcount;
		        }
		    }
		    
		    resultOffset += groupBoxCount;
	    }
	    return result;
	}


	public ParallelBoxItemGroupPermutationRotationIterator[] getIterators() {
		return workUnits;
	}

	public ParallelBoxItemGroupPermutationRotationIterator getIterator(int i) {
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
	public long getMinBoxVolume(int index) {
		return workUnits[workUnitIndex].getMinBoxVolume(index);
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

		for (ParallelBoxItemGroupPermutationRotationIterator unit : workUnits) {
			unit.removePermutations(removed);
		}
		
		for (Integer integer : removed) {
			BoxItem item = boxMatrix[integer];
			
			item.decrement();
			
			if(item.isEmpty()) {
				boxMatrix[integer] = null;
			}
		}
		
		for(int i = 0; i < groupsMatrix.length; i++) {
			if(groupsMatrix[i] == null) {
				continue;
			}
			BoxItemGroup group = groupsMatrix[i];
			group.removeEmpty();
			if(group.isEmpty()) {
				groupsMatrix[i] = null;
			}
		}			
		
		calculate();
	}

	@Override
	public long countPermutations() {
		return workUnits[0].countPermutations();
	}

	@Override
	public long[] getMinBoxVolume() {
		return workUnits[workUnitIndex].getMinBoxVolume();
	}

	@Override
	public BoxItem[] getBoxItems() {
		return workUnits[workUnitIndex].getBoxItems();
	}

	@Override
	public BoxItemGroup[] getBoxItemGroups() {
		return groupsMatrix;
	}

	@Override
	public List<BoxItemGroup> getExcludedBoxItemGroups() {
		return excluded;
	}

	@Override
	public int removeGroups(List<Integer> removed) {
		for (int i = 0; i < workUnits.length; i++) {
			workUnits[i].removeGroups(removed);
		}
		
		int count = 0;
		for (Integer i : removed) {
			BoxItemGroup boxItemGroup = groupsMatrix[i];
			for (BoxItem boxItem : boxItemGroup.getItems()) {
				count += boxItem.getCount();
				boxMatrix[boxItem.getIndex()] = null;
			}
			groupsMatrix[i] = null;
		}
		return count;
	}

}
