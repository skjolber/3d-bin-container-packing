package com.github.skjolber.packing.packer.bruteforce.reference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;

/**
 * Ported from master @ e79cf716 (4.2.4) as a reference implementation for differential tests. The recursion and
 * enumeration order are intentionally preserved - do not modernize or optimize.
 * <p>
 * Origin: {@code DefaultBoxItemPermutationRotationIterator} collapsed with the parts of
 * {@code AbstractBoxItemPermutationRotationIterator} (and the box item iterator builder) which the single-container
 * search uses. It does not implement the 5.0 iterator interface.
 * <p>
 * Rotation and permutations built into the same iterator, assuming a do-while approach:
 *
 * <pre>
 * {@code
 * do {
 * 	do {
 * 		for (int i = 0; i < n; i++) {
 * 			BoxStackValue box = instance.getStackValue(i);
 * 			// .. your code here
 * 		}
 * 	} while (instance.nextRotation() != -1);
 * } while (instance.nextPermutation() != -1);
 * }
 * </pre>
 *
 * Permutations are those of the multiset of box item indexes, i.e. a box item with a count above one occupies as many
 * positions, and equal indexes are not permuted among themselves: n! / (count(0)! * count(1)! * ...) permutations, in
 * lexicographic order. Box items which hold identical boxes are still different indexes. Rotations are the rotations
 * of each position, enumerated independently (so duplicates of a rotatable box enumerate mirrored rotation states).
 * The permutation and rotation arrays refer to the box items by their index in the input list (excluded box items
 * keep their index, but occupy no position).
 */

public class ReferencePermutationRotationIterator {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		protected int maxLoadWeight = -1;
		protected int dx = -1;
		protected int dy = -1;
		protected int dz = -1;
		protected long volume = -1L;

		protected List<BoxItem> boxItems;

		public Builder withLoadSize(int dx, int dy, int dz) {
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;

			this.volume = (long)dx * (long)dy * (long)dz;

			return this;
		}

		public Builder withMaxLoadWeight(int maxLoadWeight) {
			this.maxLoadWeight = maxLoadWeight;

			return this;
		}

		public Builder withBoxItems(List<BoxItem> stackableItems) {
			this.boxItems = stackableItems;

			return this;
		}

		public ReferencePermutationRotationIterator build() {
			if(maxLoadWeight == -1) {
				throw new IllegalStateException();
			}
			if(dx == -1 || dy == -1 || dz == -1) {
				throw new IllegalStateException();
			}
			if(boxItems == null) {
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

				// 5.0: never null, possibly empty
				List<BoxStackValue> boundRotations = box.rotations(dx, dy, dz);
				if(boundRotations.isEmpty()) {
					excluded.add(boxItem);
					continue;
				}

				// 5.0: a stack value belongs to one box, so build a box of copies. The reference iterator thus owns its
				// stack values, as the 4.x one did (it cloned them), and only the fitting rotations are numbered.
				BoxStackValue[] cloned = new BoxStackValue[boundRotations.size()];
				for (int j = 0; j < cloned.length; j++) {
					cloned[j] = boundRotations.get(j).copy();
				}
				Box clonedBox = new Box(box.getId(), box.getDescription(), box.getVolume(), box.getWeight(), cloned, Collections.emptyMap());

				included[i] = new BoxItem(clonedBox, boxItem.getCount(), i, boxItem.getGlobalIndex());
			}

			return new ReferencePermutationRotationIterator(included, excluded);
		}

	}

	protected final BoxItem[] stackableItems; // by index
	protected int[] rotations;
	protected int[] reset;

	// permutations of boxes that fit inside this container
	protected int[] permutations; // n!

	// minimum volume from index i and above
	protected long[] minBoxVolume;

	private List<BoxItem> excluded;

	public ReferencePermutationRotationIterator(BoxItem[] boxItems, List<BoxItem> excluded) {
		this.stackableItems = boxItems;

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

	/**
	 * Get number of box items within the constraints.
	 *
	 * @return number between 0 and number of {@linkplain BoxItem}s used in the constructor.
	 */

	public int boxItemLength() {
		return stackableItems.length;
	}

	public long getMinStackableArea(int offset) {
		long minArea = Long.MAX_VALUE;
		for (int i = offset; i < length(); i++) {
			BoxStackValue permutationRotation = getStackValue(i);
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
			BoxStackValue permutationRotation = getStackValue(i);
			long area = permutationRotation.getArea();
			if(area < minArea) {
				minArea = area;
				index = i;
			}
		}
		return index;
	}

	public List<BoxStackValue> get(ReferencePermutationRotationState state, int length) {
		int[] permutations = state.getPermutations();
		int[] rotations = state.getRotations();

		List<BoxStackValue> results = new ArrayList<>(length);
		for (int i = 0; i < length; i++) {
			results.add(stackableItems[permutations[i]].getBox().getStackValue(rotations[i]));
		}
		return results;
	}

	public void reset() {
		// the first permutation is in ascending order
		Arrays.sort(permutations);
		System.arraycopy(reset, 0, rotations, 0, rotations.length);
		if(permutations.length > 0) {
			calculateMinStackableVolume(0);
		}
	}

	public long countRotations() {
		int[] permutations = getPermutations();

		long n = 1;
		for (int i = 0; i < permutations.length; i++) {
			BoxItem value = stackableItems[permutations[i]];
			if(Long.MAX_VALUE / value.getBox().getStackValues().length <= n) {
				return -1L;
			}

			n = n * value.getBox().getStackValues().length;
		}
		return n;
	}

	/**
	 * Return number of permutations for boxes which fit within this container.
	 *
	 * @return permutation count
	 */

	public long countPermutations() {
		// reduce permutations for boxes which are duplicated

		// could be further bounded by looking at how many boxes (i.e. n x the smallest) which actually
		// fit within the container volume

		int[] permutations = getPermutations();

		int maxCount = 0;
		for (BoxItem value : stackableItems) {
			if(value != null) {
				if(maxCount < value.getCount()) {
					maxCount = value.getCount();
				}
			}
		}

		long n = 1;
		if(maxCount > 1) {
			int[] factors = new int[maxCount];
			for (BoxItem value : stackableItems) {
				if(value != null) {
					for (int k = 0; k < value.getCount(); k++) {
						factors[k]++;
					}
				}
			}

			for (long i = 0; i < permutations.length; i++) {
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
			for (long i = 0; i < permutations.length; i++) {
				if(Long.MAX_VALUE / (i + 1) <= n) {
					return -1L;
				}
				n = n * (i + 1);
			}
		}
		return n;
	}

	public BoxItem[] getBoxItems() {
		return stackableItems;
	}

	public long[] getMinBoxVolume() {
		return minBoxVolume;
	}

	protected void calculateMinStackableVolume(int offset) {
		BoxStackValue last = stackableItems[permutations[permutations.length - 1]].getBox().getStackValue(rotations[permutations.length - 1]);

		minBoxVolume[permutations.length - 1] = last.getVolume();

		for (int i = permutations.length - 2; i >= offset; i--) {
			long volume = stackableItems[permutations[i]].getBox().getStackValue(rotations[i]).getVolume();

			if(volume < minBoxVolume[i + 1]) {
				minBoxVolume[i] = volume;
			} else {
				minBoxVolume[i] = minBoxVolume[i + 1];
			}
		}
	}

	public BoxStackValue getStackValue(int index) {
		return stackableItems[permutations[index]].getBox().getStackValue(rotations[index]);
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

		if(permutations.length > 0) {
			calculateMinStackableVolume(0);
		}
	}

	public long getMinBoxVolume(int offset) {
		return minBoxVolume[offset];
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

	public int[] getPermutations() {
		int[] permutations = new int[this.permutations.length];
		System.arraycopy(this.permutations, 0, permutations, 0, permutations.length);
		return permutations;
	}

	public void resetRotations() {
		System.arraycopy(reset, 0, rotations, 0, rotations.length);
	}

	public int nextPermutation(int maxIndex) {
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

	public int nextPermutation() {
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

	public ReferencePermutationRotationState getState() {
		return new ReferencePermutationRotationState(rotations, permutations);
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
