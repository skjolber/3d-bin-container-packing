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


public class MutableIndexedStackableItemPermutationRotationIterator extends AbstractStackableItemPermutationRotationIterator implements StackableItems, StackableItemPermutationRotationIterator {

	public static Builder newMutableBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractStackableItemIteratorBuilder<Builder> {

		private AbstractStackableItemIteratorBuilder builder;
		
		public Builder withBuilder(AbstractStackableItemIteratorBuilder builder) {
			this.builder = builder;
			return this;
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

			StackableItemPermutationRotationIterator iterator = builder
																	.withLoadSize(size)
																	.withMaxLoadWeight(maxLoadWeight)
																	.withStackableItems(stackableItems)
																	.withFilter(filter)
																	.build();
			
			return new MutableIndexedStackableItemPermutationRotationIterator((AbstractStackableItemPermutationRotationIterator) iterator);
		}
	}

	protected List<MutableIndexedStackableItem> mutableStackableItems;
	
	protected final AbstractStackableItemPermutationRotationIterator iterator;
	
	public MutableIndexedStackableItemPermutationRotationIterator(AbstractStackableItemPermutationRotationIterator iterator) {
		super(iterator.getStackableItems());
		
		permutations = new int[0]; // n!
		
		this.iterator = iterator;
		
		resetFromIterator();
	}
	
	protected void resetFromIterator() {
		int[] permutations = iterator.getPermutations();
		
		mutableStackableItems = new ArrayList<>();
		for (int i = 0; i < stackableItems.length; i++) {
			IndexedStackableItem loadableItem = stackableItems[i];
			if(loadableItem != null && !loadableItem.isEmpty()) {
				mutableStackableItems.add(new MutableIndexedStackableItem(loadableItem));
			}
		}

		this.permutations = new int[permutations.length];
		rotations = new int[permutations.length];
		minStackableVolume = new long[permutations.length];
		
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
			calculateMutableMinStackableVolume(0);
		}
	}
	
	private void calculateMutableMinStackableVolume(int offset) {
		StackValue last = getStackValue(permutations.length - 1);

		minStackableVolume[permutations.length - 1] = last.getVolume();

		for (int i = permutations.length - 2; i >= offset; i--) {
			long volume = getStackValue(i).getVolume();

			if(volume < minStackableVolume[i + 1]) {
				minStackableVolume[i] = volume;
			} else {
				minStackableVolume[i] = minStackableVolume[i + 1];
			}
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
	
	protected AbstractStackableItemPermutationRotationIterator getIterator() {
		return iterator;
	}

	
}
