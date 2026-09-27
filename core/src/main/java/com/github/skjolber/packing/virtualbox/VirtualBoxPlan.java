package com.github.skjolber.packing.virtualbox;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;

/** Coarse-to-fine inventory partition. Nodes retain their children; every delegate attempt gets fresh inventory. */
public class VirtualBoxPlan {
	protected static class Node {
		protected final int[] inventory;
		protected final VirtualBox virtualBox;
		protected final long count;
		protected List<Node> children;

		protected Node(int[] inventory, VirtualBox virtualBox) {
			this.inventory = inventory;
			this.virtualBox = virtualBox;
			long total = 0;
			for(int i = 1; i < inventory.length; i += 2) {
				total += inventory[i];
			}
			count = total;
		}

		protected long delegateCount() { return virtualBox == null ? count : 1; }
	}

	protected final List<BoxItem> originals;
	protected final VirtualBoxLayoutCache cache;
	protected final List<Node> frontier = new ArrayList<>();
	protected final List<Node> roots;
	protected final int maxDelegateBoxes;

	protected VirtualBoxPlan(List<BoxItem> originals, VirtualBoxPacking initial, VirtualBoxLayoutCache cache, int maxDelegateBoxes) {
		this.originals = originals;
		this.cache = cache;
		this.maxDelegateBoxes = maxDelegateBoxes;
		Map<BoxItem, Integer> indexes = new IdentityHashMap<>();
		for(int i = 0; i < originals.size(); i++) {
			indexes.put(originals.get(i), i);
		}
		for(VirtualBoxPacking.Entry entry : initial.entries) {
			int[] inventory;
			if(entry.virtualBox() == null) {
				inventory = new int[] {indexes.get(entry.original()), entry.original().getCount()};
			} else {
				Map<BoxItem, Integer> counts = VirtualBox.inventory(entry.virtualBox().getLayouts().get(0));
				int[] sorted = new int[counts.size()];
				int p = 0;
				for(BoxItem item : counts.keySet()) {
					sorted[p++] = indexes.get(item);
				}
				java.util.Arrays.sort(sorted);
				inventory = new int[sorted.length * 2];
				for(int i = 0; i < sorted.length; i++) {
					inventory[2 * i] = sorted[i];
					inventory[2 * i + 1] = counts.get(originals.get(sorted[i]));
				}
				cache.put(inventory, entry.virtualBox().getLayouts());
			}
			frontier.add(new Node(inventory, entry.virtualBox()));
		}
		roots = List.copyOf(frontier);
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
		int[] physicalCounts = new int[originals.size()];
		for(Node node : frontier) {
			if(node.virtualBox != null) {
				result.add(node.virtualBox);
			} else {
				for(int i = 0; i < node.inventory.length; i += 2) {
					physicalCounts[node.inventory[i]] += node.inventory[i + 1];
				}
			}
		}
		// Reunite loose children of the same original item. Separate cloned entries
		// would change identical-item load semantics and increase permutation work.
		for(int i = 0; i < physicalCounts.length; i++) {
			if(physicalCounts[i] > 0) {
				result.add(originals.get(i), physicalCounts[i]);
			}
		}
		return result;
	}

	/**
	 * Split the largest remaining compound, breaking ties in registration order.
	 * Counts are divided, never rounded up. A child that cannot form a rectangle
	 * becomes ordinary inventory. No child search is repeated for an equal key.
	 */
	protected boolean refine(PackagerInterruptSupplier stop) {
		long currentCount = delegateCount();
		List<Node> candidates = new ArrayList<>();
		for(Node node : frontier) {
			if(node.count > 1 && (node.virtualBox != null || currentCount > maxDelegateBoxes)) {
				int p = candidates.size();
				while(p > 0 && candidates.get(p - 1).count < node.count) {
					p--;
				}
				candidates.add(p, node);
			}
		}
		for(Node node : candidates) {
			if(stop.getAsBoolean()) {
				return false;
			}
			if(node.children == null) {
				List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
				long remaining = node.count / 2;
				for(int i = 0; i < node.inventory.length; i += 2) {
					int count = node.inventory[i + 1];
					int first = (int) Math.min(remaining, count);
					if(first > 0) {
						left.add(node.inventory[i]);
						left.add(first);
						remaining -= first;
					}
					if(first < count) {
						right.add(node.inventory[i]);
						right.add(count - first);
					}
				}
				node.children = List.of(child(left, stop), child(right, stop));
			}
			long nextCount = currentCount - node.delegateCount();
			for(Node child : node.children) {
				nextCount += child.delegateCount();
			}
			if(nextCount <= maxDelegateBoxes && !stop.getAsBoolean()) {
				int index = frontier.indexOf(node);
				frontier.remove(index);
				frontier.addAll(index, node.children);
				return true;
			}
		}
		return false;
	}

	protected Node child(List<Integer> inventory, PackagerInterruptSupplier stop) {
		int[] counts = new int[inventory.size()];
		for(int i = 0; i < counts.length; i++) {
			counts[i] = inventory.get(i);
		}
		List<VirtualBoxLayout> layouts = cache.get(counts, stop);
		return new Node(counts, layouts.isEmpty() ? null : VirtualBox.of(layouts));
	}
}
