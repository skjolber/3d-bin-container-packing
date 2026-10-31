package com.github.skjolber.packing.packer.bruteforce.flat;

import java.util.Arrays;

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
		this.n = n;
		this.items = items;
		this.outInts = FlatSubtree.outInts(n);
		this.out = new int[items * outInts];
		this.outLongs = new long[items * FlatSubtree.OUT_LONGS];
	}

	public int items() {
		return items;
	}

	/**
	 * The order in which to run the items: when any best arrangement will do, the prefixes with the most volume first (a strong arrangement then
	 * bounds the others early), else their order.
	 *
	 * @param problemLongs the longs of the {@linkplain FlatProblem}
	 * @param prefixes the first permutation of each item, n values each
	 * @param depth the length of the prefixes
	 * @return the items in the order to run them
	 */
	public static int[] order(long[] problemLongs, int[] prefixes, int n, int depth, boolean anyBest) {
		int items = prefixes.length / n;
		int[] order = new int[items];
		for (int item = 0; item < items; item++) {
			order[item] = item;
		}
		if(anyBest && items < (1 << 21)) {
			// sort the items by the volume of their prefix, the largest first, keeping the order of equal ones: the keys are the volume (which is
			// below 2^43 for any box the search takes) with the item in the low bits
			long[] keys = new long[items];
			for (int item = 0; item < items; item++) {
				long volume = 0L;
				for (int i = 0; i < depth; i++) {
					volume += problemLongs[prefixes[item * n + i]];
				}
				keys[item] = ((Long.MAX_VALUE >> 21) - volume << 21) | item;
			}
			Arrays.sort(keys);
			for (int i = 0; i < items; i++) {
				order[i] = (int)(keys[i] & ((1 << 21) - 1));
			}
		}
		return order;
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

	/**
	 * The best arrangement of the items so far, written to the array: its volume, weight and count (zero if none of them has placed any box).
	 *
	 * @param best an array of three longs
	 */
	public void best(long[] best) {
		best[0] = 0L;
		best[1] = 0L;
		best[2] = 0L;
		for (int item = 0; item < items; item++) {
			int count = count(item);
			if(count > 0) {
				long volume = volume(item);
				long weight = outLongs[item * FlatSubtree.OUT_LONGS + FlatSubtree.OUT_WEIGHT];
				if(best[2] == 0L || volume > best[0] || (volume == best[0] && (weight > best[1] || (weight == best[1] && count > best[2])))) {
					best[0] = volume;
					best[1] = weight;
					best[2] = count;
				}
			}
		}
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
		return winner(winner, nodes);
	}

	/** @return the arrangement of the item (or none for -1), with the number of nodes the items have searched */
	public AcceleratorWinner winner(int winner) {
		return winner(winner, nodes());
	}

	private AcceleratorWinner winner(int winner, long nodes) {
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
