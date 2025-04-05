package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.packager.LoaderInputs;

 /**
 *
 * An iterator which also acts as {@linkplain StackableItemsS}. 
 *
 * State is restored on each remove, next rotation or next permutation.
 *
 */


public class MutableIndexedStackableItemPermutationRotationIterator extends AbstractBoxItemPermutationRotationIterator implements LoaderInputs, BoxItemPermutationRotationIterator {

	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static DelegateBuilder newBuilder(AbstractBoxItemIteratorBuilder<?> builder) {
		return new DelegateBuilder(builder);
	}
	
	public static class Builder {

		private BoxItemPermutationRotationIterator iterator;

		public Builder withIterator(BoxItemPermutationRotationIterator iterator) {
			this.iterator = iterator;
			return this;
		}
		
		public MutableIndexedStackableItemPermutationRotationIterator build() {
			return new MutableIndexedStackableItemPermutationRotationIterator(iterator);
		}
	}
	
	public static class DelegateBuilder extends AbstractBoxItemIteratorBuilder<DelegateBuilder> {

		private final AbstractBoxItemIteratorBuilder<?> builder;

		public DelegateBuilder(AbstractBoxItemIteratorBuilder<?> builder) {
			this.builder = builder;
		}
		
		public MutableIndexedStackableItemPermutationRotationIterator build() {
			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(size == null) {
				throw new IllegalStateException();
			}
			if(builder == null) {
				throw new IllegalStateException();
			}

			AbstractBoxItemPermutationRotationIterator iterator = builder
																	.withLoadSize(size)
																	.withMaxLoadWeight(maxLoadWeight)
																	.withStackableItems(stackableItems)
																	.withFilter(filter)
																	.build();
			
			return new MutableIndexedStackableItemPermutationRotationIterator(iterator);
		}
	}

	protected List<MutableIndexedStackableItem> mutableStackableItems;
	
	protected final BoxItemPermutationRotationIterator iterator;
	
	public MutableIndexedStackableItemPermutationRotationIterator(BoxItemPermutationRotationIterator iterator) {
		super(iterator.getStackableItems());
		
		permutations = new int[0]; // n!
		
		this.iterator = iterator;
		
		resetFromIterator();
	}
	
	protected void resetFromIterator() {
		mutableStackableItems = new ArrayList<>();
		for (int i = 0; i < stackableItems.length; i++) {
			BoxItem loadableItem = stackableItems[i];
			if(loadableItem != null && !loadableItem.isEmpty()) {
				mutableStackableItems.add(new MutableIndexedStackableItem(loadableItem));
			}
		}

		int[] permutations = iterator.getPermutations();
		
		this.permutations = new int[permutations.length];
		this.rotations = new int[permutations.length];
		this.minStackableVolume = new long[permutations.length];
		
		System.arraycopy(permutations, 0, this.permutations, 0, permutations.length);
		System.arraycopy(iterator.getMinStackableVolume(), 0, minStackableVolume, 0, permutations.length);
	}
	
	public BoxItem get(int index) {
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
		BoxItem loadableItem = mutableStackableItems.get(index);
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

	public List<BoxStackValue> get(PermutationRotationState state, int length) {
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
	
	protected BoxItemPermutationRotationIterator getIterator() {
		return iterator;
	}

}
