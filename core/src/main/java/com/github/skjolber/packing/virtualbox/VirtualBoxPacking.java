package com.github.skjolber.packing.virtualbox;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/** Attempt-local inventory and expansion mapping; independent of delegate cloning and local reindexing. */
public class VirtualBoxPacking {
	protected static class Entry {
		protected final BoxItem original;
		protected final VirtualBox virtualBox;

		protected Entry(BoxItem original, VirtualBox virtualBox) {
			this.original = original;
			this.virtualBox = virtualBox;
		}

		protected BoxItem original() { return original; }
		protected VirtualBox virtualBox() { return virtualBox; }
	}
	protected VirtualBoxPacking() {
	}

	protected final List<Entry> entries = new ArrayList<>();
	protected final List<BoxItem> items = new ArrayList<>();

	protected void add(BoxItem original) {
		add(original, original.getCount());
	}

	protected void add(BoxItem original, int count) {
		int index = entries.size();
		// A BoxItem clone alone rebinds the original Box's back-reference.
		Box copy = original.getBox().clone();
		items.add(new BoxItem(copy, count, -1, index));
		entries.add(new Entry(original, null));
	}

	protected void add(VirtualBox virtualBox) {
		items.add(virtualBox.toBoxItem(entries.size()));
		entries.add(new Entry(null, virtualBox));
	}

	protected List<BoxItem> getItems() {
		return items;
	}

	protected boolean hasVirtualBoxes() {
		for(Entry entry : entries) {
			if(entry.virtualBox() != null) {
				return true;
			}
		}
		return false;
	}

	protected PackagerResult expand(PackagerResult packed, List<BoxItem> originals, long start, boolean load, PackagerInterruptSupplier interrupt) {
		if(!packed.isSuccess()) {
			return new PackagerResult(List.of(), elapsed(start), packed.isTimeout(), packed.getCost());
		}
		Map<BoxItem, Integer> remaining = new IdentityHashMap<>();
		for(BoxItem item : originals) {
			remaining.put(item, item.getCount());
		}
		List<Container> containers = new ArrayList<>();
		VirtualBoxLoadValidator validator = load ? new VirtualBoxLoadValidator(interrupt) : null;
		for(Container container : packed.getContainers()) {
			Container expanded = container.clone();
			for(Placement placement : container.getStack().getPlacements()) {
				BoxStackValue value = placement.getStackValue();
				int globalIndex = value.getBox().getBoxItem().getGlobalIndex();
				if(globalIndex < 0 || globalIndex >= entries.size()) {
					throw new IllegalStateException("Delegate did not preserve operation-global box item indexes");
				}
				Entry entry = entries.get(globalIndex);
				if(entry.virtualBox() == null) {
					BoxStackValue originalValue = findOriginal(entry.original(), value);
					append(expanded, remaining, entry.original(), originalValue,
							placement.getAbsoluteX(), placement.getAbsoluteY(), placement.getAbsoluteZ());
				} else {
					int layoutIndex = value.getIndex();
					if(layoutIndex < 0 || layoutIndex >= entry.virtualBox().getLayouts().size()) {
						throw new IllegalStateException("Delegate did not preserve virtual layout indexes");
					}
					VirtualBoxLayout layout = entry.virtualBox().getLayouts().get(layoutIndex);
					var bounds = layout.getBoundingBox();
					if(value.getDx() != bounds.dx() || value.getDy() != bounds.dy() || value.getDz() != bounds.dz()) {
						throw new IllegalStateException("Delegate changed a virtual box orientation");
					}
					for(VirtualBoxPlacement child : layout.getPlacements()) {
						append(expanded, remaining, child.item(), child.stackValue(),
								(long) placement.getAbsoluteX() + child.x(), (long) placement.getAbsoluteY() + child.y(), (long) placement.getAbsoluteZ() + child.z());
					}
				}
			}
			if(expanded.getLoadWeight() > expanded.getMaxLoadWeight()) {
				throw new IllegalStateException("Expanded container exceeds its weight limit");
			}
			if(validator != null) {
				try {
					expanded = validator.validate(expanded);
				} catch(PackagerInterruptedException e) {
					return new PackagerResult(List.of(), elapsed(start), true);
				}
				if(expanded == null) {
					return new PackagerResult(List.of(), elapsed(start), false);
				}
			}
			containers.add(expanded);
		}
		for(int count : remaining.values()) {
			if(count != 0) {
				throw new IllegalStateException("Delegate result did not conserve original inventory");
			}
		}
		return new PackagerResult(containers, elapsed(start), packed.isTimeout(), packed.getCost());
	}

	protected static BoxStackValue findOriginal(BoxItem item, BoxStackValue value) {
		for(BoxStackValue original : item.getBox().getStackValues()) {
			if(original.getIndex() == value.getIndex() && original.getDx() == value.getDx()
					&& original.getDy() == value.getDy() && original.getDz() == value.getDz()) {
				return original;
			}
		}
		throw new IllegalStateException("Delegate returned an unknown original orientation");
	}

	protected static void append(Container container, Map<BoxItem, Integer> remaining, BoxItem item, BoxStackValue value, long x, long y, long z) {
		Integer count = remaining.get(item);
		if(count == null || count <= 0) {
			throw new IllegalStateException("Delegate result duplicated original inventory");
		}
		if(x < 0 || y < 0 || z < 0 || x + value.getDx() > container.getLoadDx()
				|| y + value.getDy() > container.getLoadDy() || z + value.getDz() > container.getLoadDz()) {
			throw new IllegalStateException("Expanded placement outside container");
		}
		remaining.put(item, count - 1);
		Placement placement = new Placement(value, -1, (int) x, (int) y, (int) z, false);
		placement.setIndex(container.getStack().size());
		container.getStack().add(placement);
	}

	protected static long elapsed(long start) {
		return (System.nanoTime() - start) / 1_000_000L;
	}
}
