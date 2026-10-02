package com.github.skjolber.packing.virtualbox;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;

/** Coarse-to-fine grid partition. Each node contains copies of one original item; attempts get fresh inventory. */
public class VirtualBoxPlan {
	protected static class Node {
		protected final int originalIndex;
		protected final VirtualBox virtualBox;
		protected final int count;
		protected List<Node> children;

		protected Node(int originalIndex, int count, VirtualBox virtualBox) {
			this.originalIndex = originalIndex;
			this.count = count;
			this.virtualBox = virtualBox;
		}

		protected long delegateCount() { return virtualBox == null ? count : 1; }
	}

	protected final List<BoxItem> originals;
	protected final VirtualBoxLayoutCache cache;
	protected final List<Node> frontier = new ArrayList<>();
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
			VirtualBox virtual = entry.virtualBox();
			BoxItem original = virtual == null ? entry.original() : virtual.getLayouts().get(0).getPlacements().get(0).getBoxItem();
			int index = indexes.get(original);
			int count = virtual == null ? original.getCount() : virtual.getLayouts().get(0).getPlacements().size();
			if(virtual != null) {
				cache.put(index, count, virtual.getLayouts());
			}
			frontier.add(new Node(index, count, virtual));
		}
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
				physicalCounts[node.originalIndex] += node.count;
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
	 * becomes ordinary inventory. Grid generation is not repeated for an equal key.
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
				int left = node.count / 2;
				node.children = List.of(child(node.originalIndex, left, stop), child(node.originalIndex, node.count - left, stop));
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

	protected Node child(int originalIndex, int count, PackagerInterruptSupplier stop) {
		List<VirtualBoxLayout> layouts = cache.get(originalIndex, count, stop);
		return new Node(originalIndex, count, layouts.isEmpty() ? null : VirtualBox.of(layouts));
	}
}
