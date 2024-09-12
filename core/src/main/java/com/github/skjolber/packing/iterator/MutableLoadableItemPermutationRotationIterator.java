package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.api.packager.StackableItems;

 /**
 *
 * An iterator which also acts as Loadable items. 
 *
 * State is restored on each remove, next rotation or next permutation.
 *
 */


public class MutableLoadableItemPermutationRotationIterator implements StackableItems, LoadableItemPermutationRotationIterator {

	public static Builder newMutableBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractLoadableIteratorBuilder<Builder> {

		public MutableLoadableItemPermutationRotationIterator build() {
			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(size == null) {
				throw new IllegalStateException();
			}

			IndexedStackableItem[] matrix = toMatrix();

			return new MutableLoadableItemPermutationRotationIterator(matrix);
		}
	}

	protected int[] mutableRotations; // 2^n or 6^n

	// permutations of boxes that fit inside this container
	protected int[] mutablePermutations = new int[0]; // n!

	// minimum volume from index i and above
	protected long[] mutableMinStackableVolume;

	protected List<MutableLoadableItem> mutableLoadableItems;
	
	protected final DefaultLoadableItemPermutationRotationIterator iterator;
	
	protected final IndexedStackableItem[] loadableItems; // by index
	
	public MutableLoadableItemPermutationRotationIterator(IndexedStackableItem[] loadableItems) {
		iterator = new DefaultLoadableItemPermutationRotationIterator(loadableItems);
		
		this.loadableItems = loadableItems; 
		
		resetFromIterator();
	}
	
	protected void resetFromIterator() {
		int[] permutations = iterator.getPermutations();
	
		mutableLoadableItems = new ArrayList<>();
		for (int i = 0; i < loadableItems.length; i++) {
			IndexedStackableItem loadableItem = loadableItems[i];
			if(loadableItem != null && !loadableItem.isEmpty()) {
				mutableLoadableItems.add(new MutableLoadableItem(loadableItem));
			}
		}

		mutablePermutations = new int[permutations.length];
		mutableRotations = new int[permutations.length];
		mutableMinStackableVolume = new long[permutations.length];
		
		System.arraycopy(permutations, 0, mutablePermutations, 0, permutations.length);
		System.arraycopy(iterator.getMinStackableVolume(), 0, mutableMinStackableVolume, 0, permutations.length);
	}
	
	
	public IndexedStackableItem get(int index) {
		return mutableLoadableItems.get(index);
	}
	
	@Override
	public StackValue getStackValue(int index) {
		return loadableItems[mutablePermutations[index]].getStackable().getStackValue(mutableRotations[index]);
	}

	@Override
	public int size() {
		return mutableLoadableItems.size();
	}
	
	@Override
	public int length() {
		return mutablePermutations.length;
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
		IndexedStackableItem loadableItem = mutableLoadableItems.get(index);
		loadableItem.decrement(count);
		
		if(loadableItem.isEmpty()) {
			mutableLoadableItems.remove(index);
		}
		
		int remainingCount = mutablePermutations.length - count;

		// make inline changes, do not reset
		int[] permutations = new int[remainingCount];
		int[] rotations = new int[remainingCount];
		
		int offset = 0;
		for(int i = 0; i < mutablePermutations.length; i++) {
			if(mutablePermutations[i] == loadableItem.getIndex() && count > 0) {
				count--;
			} else {
				permutations[offset] = mutablePermutations[i];
				rotations[offset] = mutableRotations[i];
				
				offset++;
			}
		}
		
		this.mutablePermutations = permutations;
		this.mutableRotations = rotations;
		
		if(remainingCount > 0) {
			calculateMutableMinStackableVolume(0);
		}
	}
	
	private void calculateMutableMinStackableVolume(int offset) {
		StackValue last = getStackValue(mutablePermutations.length - 1);

		mutableMinStackableVolume[mutablePermutations.length - 1] = last.getVolume();

		for (int i = mutablePermutations.length - 2; i >= offset; i--) {
			long volume = getStackValue(i).getVolume();

			if(volume < mutableMinStackableVolume[i + 1]) {
				mutableMinStackableVolume[i] = volume;
			} else {
				mutableMinStackableVolume[i] = mutableMinStackableVolume[i + 1];
			}
		}
	}

	public PermutationRotationState getState() {
		return new PermutationRotationState(mutableRotations, mutablePermutations);
	}
	
	@Override
	public int[] getPermutations() {
		return mutablePermutations;
	}

	public List<StackValue> get(PermutationRotationState state, int length) {
		return iterator.get(state, length);
	}

	public long getMinStackableArea(int offset) {
		long minArea = Long.MAX_VALUE;
		for (int i = offset; i < length(); i++) {
			StackValue permutationRotation = getStackValue(i);
			long area = permutationRotation.getArea();
			if(area < minArea) {
				minArea = area;
			}
		}
		return minArea;
	}

	public int getMinStackableAreaIndex(int offset) {
		long minArea = getStackValue(offset).getArea();
		int index = offset;

		for (int i = offset + 1; i < length(); i++) {
			StackValue permutationRotation = getStackValue(i);
			long area = permutationRotation.getArea();
			if(area < minArea) {
				minArea = area;
				index = i;
			}
		}
		return index;
	}

	public long getMinStackableVolume(int offset) {
		return mutableMinStackableVolume[offset];
	}
	
	protected long[] getMinStackableVolume() {
		return mutableMinStackableVolume;
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

	@Override
	public long countRotations() {
		return iterator.countRotations();
	}

	public long countMutableRotations() {
		long n = 1;
		for (int i = 0; i < mutablePermutations.length; i++) {
			IndexedStackableItem value = loadableItems[mutablePermutations[i]];
			if(Long.MAX_VALUE / value.getStackable().getStackValues().length <= n) {
				return -1L;
			}

			n = n * value.getStackable().getStackValues().length;
		}
		return n;
	}

	@Override
	public long countPermutations() {
		return iterator.countPermutations();
	}

	public long countMutablePermutations() {
		// reduce permutations for boxes which are duplicated

		// could be further bounded by looking at how many boxes (i.e. n x the smallest) which actually
		// fit within the container volume

		int maxCount = 0;
		for (IndexedStackableItem value : loadableItems) {
			if(value != null) {
				if(maxCount < value.getCount()) {
					maxCount = value.getCount();
				}
			}
		}

		long n = 1;
		if(maxCount > 1) {
			int[] factors = new int[maxCount];
			for (IndexedStackableItem value : loadableItems) {
				if(value != null) {
					for (int k = 0; k < value.getCount(); k++) {
						factors[k]++;
					}
				}
			}

			for (long i = 0; i < mutablePermutations.length; i++) {
				if(Long.MAX_VALUE / (i + 1) <= n) {
					return -1L;
				}

				n = n * (i + 1);

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
			for (long i = 0; i < mutablePermutations.length; i++) {
				if(Long.MAX_VALUE / (i + 1) <= n) {
					return -1L;
				}
				n = n * (i + 1);
			}
		}
		return n;
	}

	public void removePermutations(int removed) {
		iterator.removePermutations(removed);
		
		resetFromIterator();
	}
	
}
