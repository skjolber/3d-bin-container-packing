package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.packager.StackableItems;

 /**
 *
 * An iterator which also acts as {@linkplain StackableItemsS}. 
 *
 * State is restored on each remove, next rotation or next permutation.
 *
 */


public class MutableIndexedStackableItemGroupPermutationRotationIterator extends AbstractStackableItemGroupPermutationRotationIterator implements StackableItems, StackableItemPermutationRotationIterator {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static DelegateBuilder newBuilder(AbstractStackableItemGroupIteratorBuilder<?> builder) {
		return new DelegateBuilder(builder);
	}
	
	public static class Builder {

		private StackableItemGroupPermutationRotationIterator iterator;

		public Builder withIterator(StackableItemGroupPermutationRotationIterator iterator) {
			this.iterator = iterator;
			return this;
		}
		
		public MutableIndexedStackableItemGroupPermutationRotationIterator build() {
			return new MutableIndexedStackableItemGroupPermutationRotationIterator(iterator);
		}
	}
	
	public static class DelegateBuilder extends AbstractStackableItemGroupIteratorBuilder<DelegateBuilder> {

		private final AbstractStackableItemGroupIteratorBuilder<?> builder;

		public DelegateBuilder(AbstractStackableItemGroupIteratorBuilder<?> builder) {
			this.builder = builder;
		}
		
		public MutableIndexedStackableItemGroupPermutationRotationIterator build() {
			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(size == null) {
				throw new IllegalStateException();
			}
			if(builder == null) {
				throw new IllegalStateException();
			}

			StackableItemGroupPermutationRotationIterator iterator = builder
																	.withLoadSize(size)
																	.withMaxLoadWeight(maxLoadWeight)
																	.withStackableItemGroups(stackableItemGroups)
																	.withFilter(filter)
																	.build();
			
			return new MutableIndexedStackableItemGroupPermutationRotationIterator(iterator);
		}
	}
	
	protected List<MutableIndexedStackableItem> mutableStackableItems;
	
	protected final StackableItemGroupPermutationRotationIterator iterator;
	
	public MutableIndexedStackableItemGroupPermutationRotationIterator(StackableItemGroupPermutationRotationIterator iterator) {
		super(iterator.getStackableItems(), iterator.getGroups());
		
		permutations = new int[0]; // n!
		
		this.iterator = iterator;
		
		resetFromIterator();
	}
	
	protected void resetFromIterator() {
		mutableStackableItems = new ArrayList<>();
		for (int i = 0; i < stackableItems.length; i++) {
			IndexedStackableItem loadableItem = stackableItems[i];
			if(loadableItem != null && !loadableItem.isEmpty()) {
				mutableStackableItems.add(new MutableIndexedStackableItem(loadableItem));
			}
		}

		int[] permutations = iterator.getPermutations();
		
		this.permutations = new int[permutations.length];
		this.rotations = new int[permutations.length];
		this.minStackableVolume = new long[permutations.length];
		this.groups = iterator.getGroups();
		
		System.arraycopy(permutations, 0, this.permutations, 0, permutations.length);
		System.arraycopy(iterator.getMinStackableVolume(), 0, minStackableVolume, 0, permutations.length);
	}
	
	public IndexedStackableItem get(int index) {
		return mutableStackableItems.get(index);
	}

	@Override
	public int size() {
		return mutableStackableItems.size();
	}
	
	@Override
	public int length() {
		return permutations.length;
	}

	@Override
	public int nextPermutation(int maxIndex) {
		int result = iterator.nextPermutation(maxIndex);
		if(result != -1) {
			resetFromIterator();
		}
		return result;
	}
	
	@Override
	public int nextRotation(int maxIndex) {
		int result = iterator.nextRotation(maxIndex);
		if(result != -1) {
			resetFromIterator();
		}
		return result;
	}	
	
	@Override
	public void remove(int index, int count) {
		IndexedStackableItem loadableItem = mutableStackableItems.get(index);
		loadableItem.decrement(count);
		
		if(loadableItem.isEmpty()) {
			mutableStackableItems.remove(index);
		}
		
		int remainingCount = permutations.length - count;

		// make inline changes, do not reset
		int[] permutations = new int[remainingCount];
		int[] rotations = new int[remainingCount];
		
		int offset = 0;
		for(int i = 0; i < this.permutations.length; i++) {
			if(this.permutations[i] == loadableItem.getIndex() && count > 0) {
				count--;
			} else {
				permutations[offset] = this.permutations[i];
				rotations[offset] = this.rotations[i];
				
				offset++;
			}
		}
		
		this.permutations = permutations;
		this.rotations = rotations;
		
		if(remainingCount > 0) {
			calculateMinStackableVolume(0);
		}
	}

	public PermutationRotationState getState() {
		return new PermutationRotationState(rotations, permutations);
	}
	
	@Override
	public int[] getPermutations() {
		int[] permutations = new int[this.permutations.length];
		System.arraycopy(this.permutations, 0, permutations, 0, permutations.length);
		return permutations;
	}

	public List<StackValue> get(PermutationRotationState state, int length) {
		return iterator.get(state, length);
	}

	@Override
	public int nextRotation() {
		int result = iterator.nextRotation();
		if(result != -1) {
			resetFromIterator();
		}
		return result;
	}

	@Override
	public int nextPermutation() {
		int result = iterator.nextPermutation();
		if(result != -1) {
			resetFromIterator();
		}
		return result;
	}

	@Override
	public void removePermutations(List<Integer> removed) {
		iterator.removePermutations(removed);
		
		resetFromIterator();
	}

	public void removePermutations(int removed) {
		iterator.removePermutations(removed);
		
		resetFromIterator();
	}
	
	protected StackableItemPermutationRotationIterator getIterator() {
		return iterator;
	}

}
