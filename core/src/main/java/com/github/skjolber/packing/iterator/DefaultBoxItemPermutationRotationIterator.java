package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;

public class DefaultBoxItemPermutationRotationIterator extends AbstractBoxItemPermutationRotationIterator {
	
	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractBoxItemIteratorBuilder<Builder> {

		public DefaultBoxItemPermutationRotationIterator build() {
			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(dx == -1 || dy == -1 || dz == -1) {
				throw new IllegalStateException();
			}
			
			BoxItem[] included = new BoxItem[boxItems.size()];
			List<BoxItem> excluded = new ArrayList<>(boxItems.size());
			
			// box item and box item groups indexes are unique and static
			for (int i = 0; i < boxItems.size(); i++) {
				BoxItem boxItem = boxItems.get(i);
				
				Box box = boxItem.getBox();
				if(box.getWeight() > maxLoadWeight) {
					excluded.add(boxItem);
					continue;
				}

				if(box.getVolume() > volume) {
					excluded.add(boxItem);
					continue;
				}
				
				List<BoxStackValue> boundRotations = box.rotations(dx, dy, dz);
				if(boundRotations == null || boundRotations.isEmpty()) {
					excluded.add(boxItem);
					continue;
				}
				
				List<BoxStackValue> copied = new ArrayList<>(boundRotations.size());
				for(BoxStackValue v : boundRotations) {
					copied.add(v.copy());
				}
				Box copiedBox = new Box(box, copied);
				
				included[i] = new BoxItem(copiedBox, boxItem.getCount(), i, boxItem.getGlobalIndex()).withOrderingOf(boxItem);
			}

			return new DefaultBoxItemPermutationRotationIterator(included, excluded);
		}

	}
	
	private List<BoxItem> excluded;

	/** Whether the boxes are in a given order: there is only one permutation */
	protected boolean fixedOrder;

	/**
	 * The end positions of the blocks of boxes with the same container priority (the box items are sorted by container
	 * priority), or an empty array for a single block. Boxes are only permuted within their block, so that the boxes of a
	 * lower priority come first.
	 */
	protected int[] blockEnds = NO_BLOCKS;

	private static final int[] NO_BLOCKS = new int[0];

	public DefaultBoxItemPermutationRotationIterator(BoxItem[] boxItems, List<BoxItem> excluded) {
		super(boxItems);
		
		this.excluded = excluded;
		
		int count = 0;
		
		for (BoxItem loadableItem : boxItems) {
			if(loadableItem != null) {
				count += loadableItem.getCount();
			}
		}
		
		this.minBoxVolume = new long[count];

		initiatePermutation(count);
	}

	protected DefaultBoxItemPermutationRotationIterator(DefaultBoxItemPermutationRotationIterator source) {
		super(copyBoxItems(source.stackableItems));
		this.excluded = new ArrayList<>(source.excluded);
		this.rotations = source.rotations.clone();
		this.reset = source.reset.clone();
		this.permutations = source.permutations.clone();
		this.minBoxVolume = source.minBoxVolume.clone();
		this.fixedOrder = source.fixedOrder;
		this.blockEnds = source.blockEnds;
	}

	public void setFixedOrder(boolean fixedOrder) {
		this.fixedOrder = fixedOrder;
	}

	@Override
	public boolean isFixedOrder() {
		return fixedOrder;
	}

	public DefaultBoxItemPermutationRotationIterator fork() {
		return new DefaultBoxItemPermutationRotationIterator(this);
	}
	
	public BoxStackValue getStackValue(int index) {
		return stackableItems[permutations[index]].getBox().getStackValue(rotations[index]);
	}

	public void removePermutations(int count) {
		// discard a number of items from the front
		for(int i = 0; i < count; i++) {
			BoxItem loadableItem = stackableItems[permutations[i]];
			
			loadableItem.decrement();
			
			if(loadableItem.isEmpty()) {
				stackableItems[permutations[i]] = null;
			}
		}
		
		initiatePermutation(rotations.length - count);
	}

	protected void initiatePermutation(int remainingCount) {
		this.rotations = new int[remainingCount];
		this.reset = new int[rotations.length];

		// need to be in ascending order for the algorithm to work
		int[] permutations = new int[rotations.length];
		
		int offset = 0;
		for (int j = 0; j < stackableItems.length; j++) {
			BoxItem value = stackableItems[j];
			if(value != null && !value.isEmpty()) {
				for (int k = 0; k < value.getCount(); k++) {
					permutations[offset] = j;
					offset++;
				}
			}
		}
		
		this.permutations = permutations;
		this.blockEnds = getBlockEnds(permutations);
		
		if(permutations.length > 0) {
			calculateMinStackableVolume(0);
		}
	}

	/**
	 * @return the end positions of the blocks of boxes with the same container priority, or an empty array for a single
	 *         block
	 */
	protected int[] getBlockEnds(int[] permutations) {
		int count = 1;
		for (int i = 1; i < permutations.length; i++) {
			if(stackableItems[permutations[i]].getContainerPriority() != stackableItems[permutations[i - 1]].getContainerPriority()) {
				count++;
			}
		}
		if(count == 1) {
			return NO_BLOCKS;
		}
		int[] ends = new int[count];
		int block = 0;
		for (int i = 1; i < permutations.length; i++) {
			if(stackableItems[permutations[i]].getContainerPriority() != stackableItems[permutations[i - 1]].getContainerPriority()) {
				ends[block++] = i;
			}
		}
		ends[block] = permutations.length;
		return ends;
	}

	public long getMinBoxVolume(int offset) {
		return minBoxVolume[offset];
	}

	/**
	 * Remove permutations, if present.
	 */
	
	public void removePermutations(List<Integer> removed) {

		int count = rotations.length;
		for (Integer i : removed) {
			BoxItem boxItem = stackableItems[i];
			if(boxItem != null) {
				boxItem.decrement();
				
				count--;
				
				if(boxItem.isEmpty()) {
					stackableItems[i] = null;
				}
			}
		}
		 
		initiatePermutation(count);
	}
	
	public int nextRotation() {
		// next rotation
		return nextRotation(rotations.length - 1);
	}
	
	public int nextRotation(int maxIndex) {
		// next rotation
		for (int i = maxIndex; i >= 0; i--) {
			if(rotations[i] < stackableItems[permutations[i]].getBox().getStackValues().length - 1) {
				rotations[i]++;

				System.arraycopy(reset, 0, rotations, i + 1, rotations.length - (i + 1));

				return i;
			}
		}

		return -1;
	}
	
	@Override
	public int[] getPermutations() {
		int[] permutations = new int[this.permutations.length];
		System.arraycopy(this.permutations, 0, permutations, 0, permutations.length);
		return permutations;
	}

	public void resetRotations() {
		System.arraycopy(reset, 0, rotations, 0, rotations.length);
	}

	public int nextPermutation(int maxIndex) {
		if(fixedOrder) {
			return -1;
		}
		if(blockEnds.length != 0) {
			return nextBlockPermutation(maxIndex);
		}
		while (maxIndex >= 0) {
			int[] permutations = this.permutations;

			int current = permutations[maxIndex];

			// find the lexicographically next item to the right of the max index
			int minIndex = -1;
			for (int i = maxIndex + 1; i < permutations.length; i++) {
				if(permutations[i] > current && (minIndex == -1 || permutations[i] < permutations[minIndex])) {
					minIndex = i;
				}
			}

			// if there is no such item, decrement and try again
			if(minIndex == -1) {
				// TODO search backwards?
				maxIndex--;

				continue;
			}

			// increment to the next lexigrapically item
			// and sort the items to the right of the max index
			permutations[maxIndex] = permutations[minIndex];
			permutations[minIndex] = current;

			Arrays.sort(permutations, maxIndex + 1, permutations.length);

			resetRotations();

			calculateMinStackableVolume(maxIndex);

			return maxIndex;
		}
		return -1;
	}

	
	/**
	 * As {@link #nextPermutation(int)}, permuting the boxes within their block only: the blocks after the max index
	 * are reset to their first permutation, and when a block has no more permutations, the previous block is permuted.
	 */
	protected int nextBlockPermutation(int maxIndex) {
		int[] permutations = this.permutations;
		for (int b = blockEnds.length - 1; b >= 0; b--) {
			int start = b == 0 ? 0 : blockEnds[b - 1];
			int end = blockEnds[b];
			if(start <= maxIndex) {
				int index = Math.min(maxIndex, end - 1);
				while (index >= start) {
					int current = permutations[index];

					// find the lexicographically next item to the right of the index, within the block
					int minIndex = -1;
					for (int i = index + 1; i < end; i++) {
						if(permutations[i] > current && (minIndex == -1 || permutations[i] < permutations[minIndex])) {
							minIndex = i;
						}
					}
					if(minIndex == -1) {
						index--;
						continue;
					}
					permutations[index] = permutations[minIndex];
					permutations[minIndex] = current;
					Arrays.sort(permutations, index + 1, end);

					resetRotations();
					calculateMinStackableVolume(index);
					return index;
				}
				// continue with the previous block, at any index
				maxIndex = start - 1;
			}
			// back to the first permutation of the block
			Arrays.sort(permutations, start, end);
		}
		// back at the first permutation
		resetRotations();
		if(permutations.length > 0) {
			calculateMinStackableVolume(0);
		}
		return -1;
	}

	public int nextPermutation() {
		if(fixedOrder) {
			return -1;
		}
		if(blockEnds.length != 0) {
			resetRotations();
			return nextBlockPermutation(permutations.length - 1);
		}
		resetRotations();

		int[] permutations = this.permutations;

		// Find longest non-increasing suffix
		int i = permutations.length - 1;
		while (i > 0 && permutations[i - 1] >= permutations[i])
			i--;
		// Now i is the head index of the suffix

		// Are we at the last permutation already?
		if(i <= 0) {
			return -1;
		}

		// Let array[i - 1] be the pivot
		// Find rightmost element that exceeds the pivot
		int j = permutations.length - 1;
		while (permutations[j] <= permutations[i - 1])
			j--;
		// Now the value array[j] will become the new pivot
		// Assertion: j >= i

		int head = i - 1;

		// Swap the pivot with j
		int temp = permutations[i - 1];
		permutations[i - 1] = permutations[j];
		permutations[j] = temp;

		// Reverse the suffix
		j = permutations.length - 1;
		while (i < j) {
			temp = permutations[i];
			permutations[i] = permutations[j];
			permutations[j] = temp;
			i++;
			j--;
		}

		calculateMinStackableVolume(head);

		// Successfully computed the next permutation
		return head;
	}
	
	public int length() {
		return permutations.length;
	}
	
	public PermutationRotationState getState() {
		return new PermutationRotationState(rotations, permutations);
	}

	public BoxItem getPermutation(int index) {
		return stackableItems[permutations[index]];
	}
	
	protected int[] getRotations() {
		return rotations;
	}
	
	public List<BoxItem> getExcluded() {
		return excluded;
	}
}
