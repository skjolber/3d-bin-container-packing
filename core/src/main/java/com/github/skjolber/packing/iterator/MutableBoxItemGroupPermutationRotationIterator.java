package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;

 /**
 *
 * An iterator which also acts as {@linkplain FilteredBoxItems}. 
 *
 * Modifications done via {@linkplain FilteredBoxItems} is restored on each remove, next rotation or next permutation.
 *
 */


public class MutableBoxItemGroupPermutationRotationIterator extends AbstractBoxItemGroupPermutationRotationIterator implements FilteredBoxItems, BoxItemPermutationRotationIterator {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static DelegateBuilder newBuilder(AbstractBoxItemGroupIteratorBuilder<?> builder) {
		return new DelegateBuilder(builder);
	}
	
	public static class Builder {

		private BoxItemGroupPermutationRotationIterator iterator;

		public Builder withIterator(BoxItemGroupPermutationRotationIterator iterator) {
			this.iterator = iterator;
			return this;
		}
		
		public MutableBoxItemGroupPermutationRotationIterator build() {
			return new MutableBoxItemGroupPermutationRotationIterator(iterator);
		}
	}
	
	public static class DelegateBuilder extends AbstractBoxItemGroupIteratorBuilder<DelegateBuilder> {

		private final AbstractBoxItemGroupIteratorBuilder<?> builder;

		public DelegateBuilder(AbstractBoxItemGroupIteratorBuilder<?> builder) {
			this.builder = builder;
		}
		
		public MutableBoxItemGroupPermutationRotationIterator build() {
			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(size == null) {
				throw new IllegalStateException();
			}
			if(builder == null) {
				throw new IllegalStateException();
			}

			BoxItemGroupPermutationRotationIterator iterator = builder
																	.withLoadSize(size)
																	.withMaxLoadWeight(maxLoadWeight)
																	.withBoxItemGroups(boxItemGroups)
																	.withFilter(filter)
																	.build();
			
			return new MutableBoxItemGroupPermutationRotationIterator(iterator);
		}
	}
	
	protected List<MutableBoxItem> mutableBoxItems;
	
	protected final BoxItemGroupPermutationRotationIterator iterator;
	
	public MutableBoxItemGroupPermutationRotationIterator(BoxItemGroupPermutationRotationIterator iterator) {
		super(iterator.getStackableItems(), iterator.getGroups());
		
		permutations = new int[0]; // n!
		
		this.iterator = iterator;
		
		resetFromIterator();
	}
	
	public void resetFromIterator() {
		mutableBoxItems = new ArrayList<>();
		for (int i = 0; i < stackableItems.length; i++) {
			BoxItem loadableItem = stackableItems[i];
			if(loadableItem != null && !loadableItem.isEmpty()) {
				mutableBoxItems.add(new MutableBoxItem(loadableItem));
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
	
	public BoxItem get(int index) {
		return mutableBoxItems.get(index);
	}

	@Override
	public int size() {
		return mutableBoxItems.size();
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
		MutableBoxItem mutableBoxItem = mutableBoxItems.get(index);
		
		mutableBoxItem.decrement(count);
		
		if(mutableBoxItem.isEmpty()) {
			mutableBoxItems.remove(index);
		}
		
		int remainingCount = permutations.length - count;

		// make inline changes, do not reset
		int[] permutations = new int[remainingCount];
		int[] rotations = new int[remainingCount];
		
		int offset = 0;
		for(int i = 0; i < this.permutations.length; i++) {
			if(this.permutations[i] == mutableBoxItem.getIndex() && count > 0) {
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
