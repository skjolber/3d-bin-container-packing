package com.github.skjolber.packing.virtualbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;

/**
 * Coarse-to-fine grid partition. Each node contains copies of one original item; attempts get fresh inventory.
 * The frontier is kept normalized: one node per virtual box (with a copy count) and one loose node per
 * original item, so equal blocks are a single counted delegate item and the frontier stays small.
 */
public class VirtualBoxPlan {
	protected static class Node {
		protected final int originalIndex;
		protected final VirtualBox virtualBox;
		/** Boxes per copy; for a loose node, the number of loose boxes. */
		protected int count;
		/** Number of identical virtual boxes; always one for a loose node. */
		protected int copies;

		protected Node(int originalIndex, int count, int copies, VirtualBox virtualBox) {
			this.originalIndex = originalIndex;
			this.count = count;
			this.copies = copies;
			this.virtualBox = virtualBox;
		}

		protected long delegateCount() { return virtualBox == null ? count : copies; }
	}

	protected final List<BoxItem> originals;
	protected final VirtualBoxLayoutCache cache;
	protected final List<Node> frontier = new ArrayList<>();
	protected final int maxDelegateBoxes;

	protected VirtualBoxPlan(List<BoxItem> originals, VirtualBoxLayoutCache cache, int maxDelegateBoxes) {
		this.originals = originals;
		this.cache = cache;
		this.maxDelegateBoxes = maxDelegateBoxes;
	}

	/**
	 * Add an original item: one grid if the whole count fits, otherwise container-sized
	 * blocks, otherwise ordinary inventory.
	 */
	protected void add(int originalIndex, PackagerInterruptSupplier stop) {
		int count = originals.get(originalIndex).getCount();
		VirtualBox whole = cache.get(originalIndex, count, stop);
		if(whole != null) {
			frontier.add(new Node(originalIndex, count, 1, whole));
			return;
		}
		int[] blocks = stop.getAsBoolean() ? GridVirtualBoxLayoutGenerator.NO_BLOCKS : cache.partition(originalIndex, count);
		if(blocks.length == 0) {
			frontier.add(new Node(originalIndex, count, 1, null));
			return;
		}
		// blocks are in descending order; equal blocks become one node with copies
		int i = 0;
		while(i < blocks.length) {
			int block = blocks[i];
			int copies = 1;
			while(i + copies < blocks.length && blocks[i + copies] == block) {
				copies++;
			}
			frontier.add(node(originalIndex, block, copies, stop));
			i += copies;
		}
		normalize();
	}

	protected long delegateCount() {
		long total = 0;
		for(Node node : frontier) {
			total += node.delegateCount();
		}
		return total;
	}

	protected VirtualBoxPacking packing() {
		VirtualBoxPacking result = new VirtualBoxPacking();
		// Equal blocks are one counted item, so permutation searches do not
		// enumerate orders of interchangeable blocks.
		for(Node node : frontier) {
			if(node.virtualBox != null) {
				result.add(node.virtualBox, node.copies);
			}
		}
		// Loose boxes of the same original item are one entry. Separate cloned entries
		// would change identical-item load semantics and increase permutation work.
		for(Node node : frontier) {
			if(node.virtualBox == null) {
				result.add(originals.get(node.originalIndex), node.count);
			}
		}
		return result;
	}

	/**
	 * Split the largest remaining compound, breaking ties in frontier order. All copies of
	 * that compound are split at once, or as many as {@code maxDelegateBoxes} permits.
	 * The split halves the longest axis of the compound's preferred grid, so each child
	 * count has a sub-grid which fits a container the parent fits. The grid generator keeps
	 * a layout for every container which can hold a grid of the child count, so the child
	 * can still go where the parent went. Even splits give identical children. Grid
	 * generation is not repeated for an equal key.
	 */
	protected boolean refine(PackagerInterruptSupplier stop) {
		long currentCount = delegateCount();
		// largest count first, then frontier order
		long[] candidates = new long[frontier.size()];
		int candidateCount = 0;
		for(int i = 0; i < frontier.size(); i++) {
			Node node = frontier.get(i);
			if(node.count > 1 && node.virtualBox != null) {
				candidates[candidateCount++] = ((long) (Integer.MAX_VALUE - node.count) << 32) | i;
			}
		}
		Arrays.sort(candidates, 0, candidateCount);
		for(int c = 0; c < candidateCount; c++) {
			if(stop.getAsBoolean()) {
				return false;
			}
			int index = (int) candidates[c];
			Node node = frontier.get(index);
			int[] counts = split(node);
			Node left = node(node.originalIndex, counts[0], 1, stop);
			Node right = node(node.originalIndex, counts[1], 1, stop);
			if(stop.getAsBoolean()) {
				return false;
			}
			// each split copy replaces one delegate item with the children's items
			long increase = left.delegateCount() + right.delegateCount() - 1;
			long copies = node.copies;
			if(increase > 0) {
				copies = Math.min(copies, (maxDelegateBoxes - currentCount) / increase);
			}
			if(copies <= 0) {
				continue;
			}
			List<Node> replacement = new ArrayList<>(3);
			if(node.copies > copies) {
				replacement.add(new Node(node.originalIndex, node.count, node.copies - (int) copies, node.virtualBox));
			}
			replacement.add(scale(left, (int) copies));
			replacement.add(scale(right, (int) copies));
			frontier.remove(index);
			frontier.addAll(index, replacement);
			normalize();
			return true;
		}
		return false;
	}

	/** Halve the axis with the most cells, preferring layers, then rows, then columns. */
	protected static int[] split(Node node) {
		if(node.virtualBox.getLayouts().get(0) instanceof GridVirtualBoxLayoutGenerator.GridLayout grid) {
			int cells = grid.layers;
			int other = grid.columns * grid.rows;
			if(grid.rows > cells) {
				cells = grid.rows;
				other = grid.columns * grid.layers;
			}
			if(grid.columns > cells) {
				cells = grid.columns;
				other = grid.rows * grid.layers;
			}
			return new int[] {(cells + 1) / 2 * other, cells / 2 * other};
		}
		int left = node.count / 2;
		return new int[] {node.count - left, left};
	}

	/** A node for {@code copies} blocks of {@code count} boxes; loose if the count forms no grid. */
	protected Node node(int originalIndex, int count, int copies, PackagerInterruptSupplier stop) {
		VirtualBox virtualBox = cache.get(originalIndex, count, stop);
		if(virtualBox == null) {
			return new Node(originalIndex, count * copies, 1, null);
		}
		return new Node(originalIndex, count, copies, virtualBox);
	}

	protected static Node scale(Node node, int copies) {
		if(node.virtualBox == null) {
			return new Node(node.originalIndex, node.count * copies, 1, null);
		}
		return new Node(node.originalIndex, node.count, node.copies * copies, node.virtualBox);
	}

	/** Merge nodes sharing a virtual box, and loose nodes of the same original item, keeping first positions. */
	protected void normalize() {
		List<Node> merged = new ArrayList<>(frontier.size());
		Map<VirtualBox, Node> virtualNodes = new IdentityHashMap<>();
		Node[] looseNodes = new Node[originals.size()];
		for(Node node : frontier) {
			if(node.virtualBox != null) {
				Node existing = virtualNodes.get(node.virtualBox);
				if(existing == null) {
					existing = new Node(node.originalIndex, node.count, node.copies, node.virtualBox);
					virtualNodes.put(node.virtualBox, existing);
					merged.add(existing);
				} else {
					existing.copies += node.copies;
				}
			} else {
				Node existing = looseNodes[node.originalIndex];
				if(existing == null) {
					existing = new Node(node.originalIndex, node.count, 1, null);
					looseNodes[node.originalIndex] = existing;
					merged.add(existing);
				} else {
					existing.count += node.count;
				}
			}
		}
		frontier.clear();
		frontier.addAll(merged);
	}
}
