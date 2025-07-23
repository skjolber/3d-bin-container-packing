package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.packager.FilteredBoxItemGroups;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;

 /**
 *
 * An iterator which also acts as {@linkplain FilteredBoxItems}. 
 *
 * State is restored on each remove, next rotation or next permutation.
 *
 */

public class FilteredBoxItemsPermutationRotationIterator extends AbstractBoxItemPermutationRotationIterator implements FilteredBoxItems {

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
		
		public FilteredBoxItemsPermutationRotationIterator build() {
			return new FilteredBoxItemsPermutationRotationIterator(iterator);
		}
	}
	
	public static class DelegateBuilder extends AbstractBoxItemIteratorBuilder<DelegateBuilder> {

		private final AbstractBoxItemIteratorBuilder<?> builder;

		public DelegateBuilder(AbstractBoxItemIteratorBuilder<?> builder) {
			this.builder = builder;
		}
		
		public FilteredBoxItemsPermutationRotationIterator build() {
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
																	.withBoxItems(boxItems)
																	.withFilter(filter)
																	.build();
			
			return new FilteredBoxItemsPermutationRotationIterator(iterator);
		}
	}

	protected List<BoxItem> boxItems;
	
	protected final BoxItemPermutationRotationIterator iterator;
	
	public FilteredBoxItemsPermutationRotationIterator(BoxItemPermutationRotationIterator iterator) {
		super(iterator.getBoxItems());
		
		permutations = new int[0]; // n!
		
		this.iterator = iterator;
		
		resetFromIterator();
	}
	
	protected void resetFromIterator() {
		boxItems = new ArrayList<>();
		for (int i = 0; i < stackableItems.length; i++) {
			BoxItem loadableItem = stackableItems[i];
			if(loadableItem != null && !loadableItem.isEmpty()) {
				boxItems.add(loadableItem.clone());
			}
		}

		int[] permutations = iterator.getPermutations();
		
		this.permutations = new int[permutations.length];
		this.rotations = new int[permutations.length];
		this.minBoxVolume = new long[permutations.length];
		
		System.arraycopy(permutations, 0, this.permutations, 0, permutations.length);
		System.arraycopy(iterator.getMinBoxVolume(), 0, minBoxVolume, 0, permutations.length);
	}
	
	public BoxItem get(int index) {
		return boxItems.get(index);
	}

	@Override
	public int size() {
		return boxItems.size();
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
	public boolean decrement(int index, int count) {
		BoxItem mutableBoxItem = boxItems.get(index);
		mutableBoxItem.decrement(count);
		
		if(mutableBoxItem.isEmpty()) {
			boxItems.remove(index);
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
		
		return !boxItems.isEmpty();
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

	@Override
	public boolean isEmpty() {
		return boxItems.isEmpty();
	}

	@Override
	public BoxItem remove(int index) {
		BoxItem boxItem = boxItems.get(index);
		decrement(index, boxItem.getCount());
		return boxItem;
	}

	public void removeEmpty() {
		int remainingCount = 0;
		for (BoxItem boxItem : boxItems) {
			remainingCount += boxItem.getCount();
			boxItem.mark();
		}
		
		// make inline changes, do not reset
		int[] permutations = new int[remainingCount];
		int[] rotations = new int[remainingCount];
		
		int offset = 0;
		for(int i = 0; i < this.permutations.length; i++) {
			
			if(stackableItems[this.permutations[i]].isEmpty()) {
				continue;
			}
			stackableItems[i].decrement();
			
			permutations[offset] = this.permutations[i];
			rotations[offset] = this.rotations[i];
			
			offset++;
		}
		
		this.permutations = permutations;
		this.rotations = rotations;
		
		for (BoxItem boxItem : boxItems) {
			boxItem.reset();
		}

		boxItems = new ArrayList<>();
		for (int i = 0; i < stackableItems.length; i++) {
			BoxItem loadableItem = stackableItems[i];
			if(loadableItem != null && !loadableItem.isEmpty()) {
				boxItems.add(loadableItem.clone());
			}
		}

		if(remainingCount > 0) {
			calculateMinStackableVolume(0);
		}
	}

	@Override
	public Iterator<BoxItem> iterator() {
		return boxItems.listIterator();
	}

	@Override
	public FilteredBoxItemGroups getGroups() {
		return null;
	}

}
