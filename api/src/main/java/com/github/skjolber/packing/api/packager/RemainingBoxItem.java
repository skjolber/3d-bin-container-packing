package com.github.skjolber.packing.api.packager;

import java.io.Serializable;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;

/**
 * A box item within a packaging operation: the number of its boxes which remain to be packed, and its indexes.
 * Packing changes these, not the {@link BoxItem}, which is the input: placements refer to the box item, and the box
 * items of the input are not modified.
 */
public class RemainingBoxItem implements Serializable {

	private static final long serialVersionUID = 1L;

	protected final BoxItem boxItem;
	protected final Box box;
	protected int count;
	/** Dense, mutable position used by iterators and {@link BoxItemSource}s */
	protected int localIndex = -1;
	/** Identity of the box item within one packaging operation */
	protected int globalIndex = -1;
	protected int resetCount;
	protected RemainingBoxItemGroup group;

	/**
	 * @param boxItem the box item, with all its boxes remaining and no indexes
	 */
	public RemainingBoxItem(BoxItem boxItem) {
		this(boxItem, boxItem.getCount(), -1, -1);
	}

	public RemainingBoxItem(BoxItem boxItem, int count, int localIndex, int globalIndex) {
		this.boxItem = boxItem;
		this.box = boxItem.getBox();
		this.count = count;
		this.resetCount = count;
		this.localIndex = localIndex;
		this.globalIndex = globalIndex;
	}

	/**
	 * @return the box item (the input)
	 */
	public BoxItem getBoxItem() {
		return boxItem;
	}

	public Box getBox() {
		return box;
	}

	/**
	 * @return the number of boxes which remain
	 */
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public boolean decrement() {
		count--;
		return count > 0;
	}

	public boolean decrement(int value) {
		this.count = this.count - value;
		return count > 0;
	}

	public boolean isEmpty() {
		return count == 0;
	}

	/**
	 * @return a copy of the remaining count and indexes, for the same box item (not in a group)
	 */
	public RemainingBoxItem copy() {
		return new RemainingBoxItem(boxItem, count, localIndex, globalIndex);
	}

	/**
	 * Set the dense index used by the current iterator or {@code BoxItemSource}. This is not an operation-wide
	 * identity and may change after filtering.
	 */
	public void setLocalIndex(int localIndex) {
		this.localIndex = localIndex;
	}

	/**
	 * Return the dense index used by the current iterator or {@code BoxItemSource}. This is not an operation-wide
	 * identity and may change after filtering.
	 */
	public int getLocalIndex() {
		return localIndex;
	}

	/**
	 * @return the identity of the box item within the packaging operation
	 */
	public int getGlobalIndex() {
		return globalIndex;
	}

	public void setGlobalIndex(int globalIndex) {
		this.globalIndex = globalIndex;
	}

	/**
	 * @return the volume of the boxes which remain
	 */
	public long getVolume() {
		return count * box.getVolume();
	}

	/**
	 * @return the weight of the boxes which remain
	 */
	public long getWeight() {
		return (long)count * box.getWeight();
	}

	public void reset() {
		this.count = resetCount;
	}

	public void setResetCount(int resetCount) {
		this.resetCount = resetCount;
	}

	public void decrementResetCount() {
		this.resetCount--;
	}

	public void mark() {
		this.resetCount = count;
	}

	public void setGroup(RemainingBoxItemGroup group) {
		this.group = group;
	}

	public RemainingBoxItemGroup getGroup() {
		return group;
	}

	/**
	 * Identify the group within the packaging operation: the copies of a group made during packing share the group
	 * which they were made from.
	 *
	 * @return the box item group (the input), or null when not packing groups
	 */
	public Object getGroupKey() {
		if(group == null) {
			return null;
		}
		return group.getBoxItemGroup() != null ? group.getBoxItemGroup() : group;
	}

	public int getContainerPriority() {
		return boxItem.getContainerPriority();
	}

	public int getExtractionOrder() {
		return boxItem.getExtractionOrder();
	}

	public boolean isMaxLoad() {
		return box.isMaxLoad();
	}

	@Override
	public String toString() {
		return String.format("%dx%s #%d", count, box, localIndex);
	}
}
