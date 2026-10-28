package com.github.skjolber.packing.packer.bruteforce.flat;

/**
 * The output of all items of a search (see {@linkplain FlatSubtree}), and what the host does with it between runs: find the items which still have
 * to run, and reduce the best arrangements of the items to the winner. Shared by the engines which run the items, on threads or on a device.
 */
public final class FlatItems {

	private final int n;
	private final int items;
	private final int outInts;
	private final int[] out;
	private final long[] outLongs;

	/**
	 * @param n the number of boxes of a permutation
	 * @param items the number of items
	 */
	public FlatItems(int n, int items) {
		this(n, items, FlatSubtree.outInts(n));
	}

	/**
	 * @param n the number of boxes of a permutation
	 * @param items the number of items
	 * @param outInts the ints of the output of an item: at least {@link FlatSubtree#outInts(int)}, with that layout first ({@link FlatTreeItem#outInts(int)}
	 *        for the tree)
	 */
	public FlatItems(int n, int items, int outInts) {
		this.n = n;
		this.items = items;
		this.outInts = outInts;
		this.out = new int[items * outInts];
		this.outLongs = new long[items * FlatSubtree.OUT_LONGS];
	}

	public int items() {
		return items;
	}

	/** @return the output ints of all items, {@link #outInts()} for each */
	public int[] out() {
		return out;
	}

	public int outInts() {
		return outInts;
	}

	/** @return the output longs of all items, {@link FlatSubtree#OUT_LONGS} for each */
	public long[] outLongs() {
		return outLongs;
	}

	public int status(int item) {
		return out[item * outInts + FlatSubtree.OUT_STATUS];
	}

	public int count(int item) {
		return out[item * outInts + FlatSubtree.OUT_COUNT];
	}

	public long volume(int item) {
		return outLongs[item * FlatSubtree.OUT_LONGS + FlatSubtree.OUT_VOLUME];
	}

	/** @return true if the item has searched all its permutations, or one of them places all boxes */
	public boolean isDone(int item) {
		return status(item) == FlatSubtree.DONE;
	}

	/** @return true if the item is done and places all boxes */
	public boolean isFull(int item) {
		return isDone(item) && out[item * outInts + FlatSubtree.OUT_FULL] != 0;
	}

	/** @return true if an item needed more free points than the layout holds, so that the problem cannot be searched with the layout */
	public boolean isOverflow() {
		for (int item = 0; item < items; item++) {
			if(status(item) == FlatSubtree.OVERFLOW) {
				return true;
			}
		}
		return false;
	}

	/** @return the first item which is done and places all boxes, or the number of items if there is none */
	public int fullItem() {
		for (int item = 0; item < items; item++) {
			if(isFull(item)) {
				return item;
			}
		}
		return items;
	}

	/**
	 * Find the items which have to run: those before the first item which places all boxes, and which are not done.
	 *
	 * @param fullItem the first item which places all boxes, see {@link #fullItem()}
	 * @param pending the array to write the items to, in ascending order
	 * @return the number of items written
	 */
	public int pending(int fullItem, int[] pending) {
		int count = 0;
		for (int item = 0; item < fullItem; item++) {
			if(!isDone(item)) {
				pending[count++] = item;
			}
		}
		return count;
	}

	/** @return the most load volume of the best arrangements of the items so far, or zero if none of them has placed any box */
	public long bestVolume() {
		long best = 0L;
		for (int item = 0; item < items; item++) {
			if(count(item) > 0) {
				best = Math.max(best, volume(item));
			}
		}
		return best;
	}

	/** @return the number of nodes the items have searched */
	public long nodes() {
		long nodes = 0L;
		for (int item = 0; item < items; item++) {
			nodes += outLongs[item * FlatSubtree.OUT_LONGS + FlatSubtree.OUT_NODES];
		}
		return nodes;
	}

	/** @return the best arrangement of the items in their order, where the first arrangement wins ties, or the first which places all boxes */
	public AcceleratorWinner reduce() {
		long nodes = nodes();

		int winner = -1;
		for (int item = 0; item < items; item++) {
			if(!isDone(item)) {
				// after the first item which places all boxes
				break;
			}
			if(count(item) == 0) {
				continue;
			}
			if(winner == -1 || isBetter(item, winner)) {
				winner = item;
			}
			if(isFull(item)) {
				break;
			}
		}
		if(winner == -1) {
			return new AcceleratorWinner(new int[n], new int[n], 0, 0L, 0L, nodes, false);
		}

		int base = winner * outInts;
		int[] permutation = new int[n];
		System.arraycopy(out, base + FlatSubtree.outBestPermutation(n), permutation, 0, n);
		int count = out[base + FlatSubtree.OUT_COUNT];
		int[] rotations = new int[n];
		System.arraycopy(out, base + FlatSubtree.outBestRotations(n), rotations, 0, count);
		int longBase = winner * FlatSubtree.OUT_LONGS;
		return new AcceleratorWinner(permutation, rotations, count, outLongs[longBase + FlatSubtree.OUT_VOLUME], outLongs[longBase + FlatSubtree.OUT_WEIGHT], nodes, isFull(winner));
	}

	/** @return true if the arrangement of the item is strictly better than the arrangement of the other item: more load volume, then more load weight, then more boxes */
	private boolean isBetter(int item, int other) {
		long volume = outLongs[item * FlatSubtree.OUT_LONGS + FlatSubtree.OUT_VOLUME];
		long otherVolume = outLongs[other * FlatSubtree.OUT_LONGS + FlatSubtree.OUT_VOLUME];
		if(volume != otherVolume) {
			return volume > otherVolume;
		}
		long weight = outLongs[item * FlatSubtree.OUT_LONGS + FlatSubtree.OUT_WEIGHT];
		long otherWeight = outLongs[other * FlatSubtree.OUT_LONGS + FlatSubtree.OUT_WEIGHT];
		if(weight != otherWeight) {
			return weight > otherWeight;
		}
		return count(item) > count(other);
	}
}
