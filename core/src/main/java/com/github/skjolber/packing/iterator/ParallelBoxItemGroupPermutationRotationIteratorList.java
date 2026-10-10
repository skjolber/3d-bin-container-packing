package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;

/**
 * 
 * This class is responsible for splitting the work load (as in the permutations) over multiple iterators.
 * <br>
 * <br>
 * The work units are created when first requested (see {@link #getIterator(int)}): the first and the last permutation of
 * a work unit follow from the remaining boxes and its index alone, so a work unit which is not searched costs nothing.
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
			if(dx == -1 || dy == -1 || dz == -1) {
				throw new IllegalStateException();
			}

			BoxItemGroupMatrix matrix = toMatrix();
			return new ParallelBoxItemGroupPermutationRotationIteratorList(matrix.groups(), matrix.boxItems(), matrix.stackValues(), matrix.excluded(), parallelizationCount);
		}
	}
	
	/**
	 * Work units for the remaining boxes of an iterator. The work units share the iterator's rotations, which are
	 * computed once for both (see {@linkplain Builder#build()}).
	 *
	 * @param iterator the iterator
	 * @param parallelizationCount the number of work units
	 * @return the list of work units
	 */
	public static ParallelBoxItemGroupPermutationRotationIteratorList of(DefaultBoxItemGroupPermutationRotationIterator iterator, int parallelizationCount) {
		AbstractBoxItemGroupsPermutationRotationIterator.GroupState state = AbstractBoxItemGroupsPermutationRotationIterator.copyGroupState(iterator);
		return new ParallelBoxItemGroupPermutationRotationIteratorList(state.groups(), state.boxes(), iterator.stackValues, new ArrayList<>(iterator.excludedBoxItemGroups), parallelizationCount);
	}

	protected final int[] frequencies;
	/** The work units, each created when first requested (see {@link #getIterator(int)}) */
	protected final ParallelBoxItemGroupPermutationRotationIterator[] workUnits;

	protected int workUnitIndex = 0;
	
	protected BoxItemGroup[] groupsMatrix;
	protected BoxItem[] boxMatrix;
	/** The rotations of each box item which fit the container, by index; not modified */
	protected final BoxStackValue[][] stackValues;
	protected List<BoxItemGroup> excluded;

	// the split of the remaining boxes, see calculate()
	private int count;
	private long permutationCount;
	/** The first rotations of the work units: all zero, shared (read-only) between the work units */
	private int[] reset;

	public ParallelBoxItemGroupPermutationRotationIteratorList(BoxItemGroup[] boxItemGroups, BoxItem[] boxItems, BoxStackValue[][] stackValues, List<BoxItemGroup> excluded, int parallelizationCount) {
		this.stackValues = stackValues;
		this.workUnits = new ParallelBoxItemGroupPermutationRotationIterator[parallelizationCount];
		this.excluded = excluded;
		this.groupsMatrix = boxItemGroups;
		this.boxMatrix = boxItems;

		// the boxes of the groups
		this.frequencies = new int[boxItems.length];
		for(BoxItemGroup group : boxItemGroups) {
			if(group == null) {
				// excluded, i.e. does not fit the container
				continue;
			}
			for(int l = 0; l < group.size(); l++) {
				BoxItem item = group.get(l);
				frequencies[item.getLocalIndex()] = item.getCount();
			}
		}

		calculate();
	}

	private ParallelBoxItemGroupPermutationRotationIteratorList(ParallelBoxItemGroupPermutationRotationIteratorList source) {
		this.frequencies = source.frequencies.clone();
		this.workUnitIndex = source.workUnitIndex;
		this.stackValues = source.stackValues;
		this.boxMatrix = AbstractBoxItemPermutationRotationIterator.copyBoxItems(source.boxMatrix);
		this.groupsMatrix = new BoxItemGroup[source.groupsMatrix.length];
		for(int i = 0; i < groupsMatrix.length; i++) {
			BoxItemGroup group = source.groupsMatrix[i];
			if(group != null) {
				List<BoxItem> items = new ArrayList<>(group.size());
				for(BoxItem item : group.getItems()) {
					items.add(boxMatrix[item.getLocalIndex()]);
				}
				groupsMatrix[i] = new BoxItemGroup(group.getId(), items, group.getIndex());
			}
		}
		this.excluded = new ArrayList<>(source.excluded);
		this.count = source.count;
		this.permutationCount = source.permutationCount;
		this.reset = source.reset;
		// the work units which exist (the others are created from the frequencies, at their first permutation)
		this.workUnits = new ParallelBoxItemGroupPermutationRotationIterator[source.workUnits.length];
		for(int i = 0; i < workUnits.length; i++) {
			if(source.workUnits[i] != null) {
				workUnits[i] = source.workUnits[i].fork();
			}
		}
	}

	public ParallelBoxItemGroupPermutationRotationIteratorList fork() {
		return new ParallelBoxItemGroupPermutationRotationIteratorList(this);
	}
	
	/**
	 * Back to the first permutations and rotations of the work units which exist, see {@link BoxItemGroupPermutationRotationIterator#reset()}.
	 */
	@Override
	public void reset() {
		// the list as a single iterator: from the first work unit again
		workUnitIndex = 0;
		// the work units again from their first permutations
		for (ParallelBoxItemGroupPermutationRotationIterator workUnit : workUnits) {
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

		count = AbstractBoxItemGroupsPermutationRotationIterator.getBoxCount(groupsMatrix);
		permutationCount = AbstractBoxItemGroupsPermutationRotationIterator.countPermutations(groupsMatrix);

		if(count == 0) {
			return;
		}

		if(permutationCount == -1L) {
			throw new IllegalArgumentException();
		}

		reset = new int[PADDING + count];
	}

	/**
	 * Create a work unit: it starts at the first permutation of its share of the permutations, and ends at the first
	 * permutation of the next work unit (the last work unit ends at the last permutation).
	 * <br>
	 * This only reads the state of the list, so different work units can be created from different threads.
	 */
	private ParallelBoxItemGroupPermutationRotationIterator createWorkUnit(int index) {
		// copy working variables so threads are less of the same
		// memory area as one another
		BoxItem[] boxMatrixCopy = new BoxItem[boxMatrix.length];			

		BoxItemGroup[] groupsMatrixCopy = new BoxItemGroup[groupsMatrix.length];
		for(int k = 0; k < groupsMatrixCopy.length; k++) {
			if(groupsMatrix[k] == null) {
				// excluded, i.e. does not fit the container
				continue;
			}
			groupsMatrixCopy[k] = groupsMatrix[k].copy();
			
			for(int l = 0; l < groupsMatrixCopy[k].size(); l++) {
				BoxItem item =  groupsMatrixCopy[k].get(l);
				boxMatrixCopy[item.getLocalIndex()] = item;
			}
		}
		
		ParallelBoxItemGroupPermutationRotationIterator workUnit = new ParallelBoxItemGroupPermutationRotationIterator(groupsMatrixCopy, boxMatrixCopy, stackValues, excluded);
		if(count == 0) {
			// no boxes
			workUnit.initiatePermutations();
			return workUnit;
		}

		workUnit.setPermutations(firstPermutation(index));
		workUnit.setRotations(new int[reset.length]);
		workUnit.setReset(reset);
		workUnit.initMinStackableVolume();

		if(index < workUnits.length - 1) {
			// the first permutation of the next work unit (array with padding)
			workUnit.setLastPermutation(firstPermutation(index + 1));
		}
		return workUnit;
	}

	/**
	 * @return the first permutation (array with padding) of a work unit
	 */
	private int[] firstPermutation(int index) {
		long rank = (permutationCount * index) / workUnits.length;

		rank++;

		return unrank(frequencies.clone(), count, permutationCount, rank, groupsMatrix);
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
		        	
					int index = item.getLocalIndex();
		        	
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


	/**
	 * @return all work units, creating those which do not exist yet
	 */
	public ParallelBoxItemGroupPermutationRotationIterator[] getIterators() {
		for (int i = 0; i < workUnits.length; i++) {
			getIterator(i);
		}
		return workUnits;
	}

	/**
	 * Get a work unit, created when first requested. Different threads can request different work units, as long as the
	 * remaining boxes are not changed ({@linkplain #removePermutations(List)}, {@linkplain #removeGroups(List)}) concurrently.
	 *
	 * @param i work unit index
	 * @return the work unit
	 */
	public ParallelBoxItemGroupPermutationRotationIterator getIterator(int i) {
		ParallelBoxItemGroupPermutationRotationIterator workUnit = workUnits[i];
		if(workUnit == null) {
			workUnit = createWorkUnit(i);
			workUnits[i] = workUnit;
		}
		return workUnit;
	}

	/**
	 * @return the work unit which this list iterates (as a single iterator)
	 */
	private ParallelBoxItemGroupPermutationRotationIterator current() {
		return getIterator(workUnitIndex);
	}

	public int length() {
		return current().length();
	}
	
	@Override
	public BoxStackValue getStackValue(int index) {
		return current().getStackValue(index);
	}

	@Override
	public BoxItem getBoxItem(int index) {
		return current().getBoxItem(index);
	}

	@Override
	public BoxStackValue[] getStackValues(int index) {
		return current().getStackValues(index);
	}

	@Override
	public BoxStackValue[][] getBoxItemStackValues() {
		return stackValues;
	}

	@Override
	public PermutationRotationState getState() {
		return current().getState();
	}

	@Override
	public List<BoxStackValue> get(PermutationRotationState state, int length) {
		return current().get(state, length);
	}

	@Override
	public long getMinBoxVolume(int index) {
		return current().getMinBoxVolume(index);
	}

	@Override
	public int[] getPermutations() {
		return current().getPermutations();
	}

	@Override
	public long countRotations() {
		return current().countRotations();
	}

	@Override
	public int nextRotation() {
		return current().nextRotation();
	}

	@Override
	public int nextRotation(int maxIndex) {
		return current().nextRotation(maxIndex);
	}

	@Override
	public int nextPermutation() {
		while(workUnitIndex < workUnits.length) {
			int nextPermutation = current().nextPermutation();
			
			if(nextPermutation != -1) {
				return nextPermutation;
			}

			// compare previous permutation to the next
			workUnitIndex++;
			if(workUnitIndex < workUnits.length) {
				int[] permutations = current().getPermutations();

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
			int nextPermutation = current().nextPermutation(maxIndex);
			
			if(nextPermutation != -1) {
				return nextPermutation;
			}
			// compare previous permutation to the next
			workUnitIndex++;
			if(workUnitIndex < workUnits.length) {
				int[] permutations = current().getPermutations();

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
		int[] permutations = current().getPermutations();
		
		List<Integer> removed = new ArrayList<>(permutations.length);
		
		for(int i = 0; i < count; i++) {
			removed.add(permutations[i]);
		}
		
		removePermutations(removed);
	}
	
	public void removePermutations(List<Integer> removed) {
		// the work units are created again from the remaining boxes (see calculate())
		for (Integer integer : removed) {
			if(frequencies[integer] > 0) {
				frequencies[integer]--;
			}
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
		return permutationCount;
	}

	@Override
	public long[] getMinBoxVolume() {
		return current().getMinBoxVolume();
	}

	@Override
	public BoxItem[] getBoxItems() {
		return current().getBoxItems();
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
		int count = 0;
		for (Integer i : removed) {
			BoxItemGroup boxItemGroup = groupsMatrix[i];
			if(boxItemGroup == null) {
				// excluded, i.e. does not fit the container
				continue;
			}
			for (BoxItem boxItem : boxItemGroup.getItems()) {
				count += boxItem.getCount();
				boxMatrix[boxItem.getLocalIndex()] = null;
			}
			groupsMatrix[i] = null;
		}
		// split the permutations of the remaining groups between the work units (created again when requested)
		calculate();
		return count;
	}

}
