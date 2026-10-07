package com.github.skjolber.packing.api;

import java.io.Serializable;

/**
 * A {@linkplain Box} repeated one or more times. Typically corresponding to an
 * order-line, but can also represent multiple products which share the same
 * size.
 * 
 */
public class BoxItem implements Serializable {

	private static final long serialVersionUID = 1L;

	protected int count;
	protected final Box box;
	/** Dense, mutable position used by iterator implementations. */
	protected int localIndex = -1;
	/** Immutable identity of this box item within one packaging operation. */
	protected int globalIndex = -1;

	protected int resetCount;
	protected BoxItemGroup group;

	/** Containers are filled in order of priority, see {@link #withContainerPriority(int)} */
	protected int containerPriority;
	/** Boxes are extracted in order, see {@link #withExtractionOrder(int)} */
	protected int extractionOrder;

	public BoxItem(Box box) {
		this(box, 1);
	}

	public BoxItem(Box box, int count) {
		super();
		this.box = box;
		this.count = count;

		this.resetCount = count;
	}

	public BoxItem(Box box, int count, int localIndex) {
		this(box, count, localIndex, -1);
	}

	public BoxItem(Box box, int count, int localIndex, int globalIndex) {
		super();
		this.box = box;
		this.count = count;
		this.localIndex = localIndex;
		this.globalIndex = globalIndex;

		this.resetCount = count;
	}

	public int getCount() {
		return count;
	}

	public Box getBox() {
		return box;
	}

	@Override
	public String toString() {
		return String.format("%dx%s #%d", count, box, localIndex);
	}

	public boolean decrement() {
		count--;
		return count > 0;
	}

	public boolean isEmpty() {
		return count == 0;
	}

	public boolean decrement(int value) {
		this.count = this.count - value;
		return count > 0;
	}

	public BoxItem copy() {
		return new BoxItem(box, count, localIndex, globalIndex).withOrderingOf(this);
	}

	/**
	 * Set the container priority: a box must not be in a later container than a box with a higher priority (a lower
	 * value). The containers of a result hold a contiguous range of priorities, and a priority only starts in a
	 * container once all boxes of the lower values are placed in it or in earlier containers. Default 0; boxes with
	 * equal values can be in any container.
	 *
	 * @param containerPriority priority, lower values in earlier containers
	 * @return this box item
	 */
	public BoxItem withContainerPriority(int containerPriority) {
		this.containerPriority = containerPriority;
		return this;
	}

	/**
	 * Set the extraction order: within a container, a box can be extracted before the boxes with a later order (a
	 * higher value), without moving them: none of them rests on it, or is in its path to the container's opening (see
	 * {@link ContainerAccess}). Default 0; boxes with equal values can be extracted in any order, for example the boxes
	 * of one delivery stop.
	 *
	 * @param extractionOrder order, lower values extracted first
	 * @return this box item
	 */
	public BoxItem withExtractionOrder(int extractionOrder) {
		this.extractionOrder = extractionOrder;
		return this;
	}

	/**
	 * Copy the container priority and extraction order of another box item.
	 *
	 * @param other the box item to copy from
	 * @return this box item
	 */
	public BoxItem withOrderingOf(BoxItem other) {
		this.containerPriority = other.containerPriority;
		this.extractionOrder = other.extractionOrder;
		return this;
	}

	/**
	 * Identify the box item's group. Box items and groups are copied during packing, and a box refers to its latest
	 * box item copy, so groups are identified by their index, which the copies keep.
	 *
	 * @return the group's index, the group itself if it has no index, or null without a group
	 */
	public Object getGroupKey() {
		if(group == null) {
			return null;
		}
		return group.getIndex() >= 0 ? (Object)Integer.valueOf(group.getIndex()) : group;
	}

	public int getContainerPriority() {
		return containerPriority;
	}

	public int getExtractionOrder() {
		return extractionOrder;
	}

	/**
	 * Set the dense index used by the current iterator or {@code BoxItemSource}.
	 * This is not an operation-wide identity and may change after filtering.
	 */
	public void setLocalIndex(int localIndex) {
		this.localIndex = localIndex;
	}

	/**
	 * Return the dense index used by the current iterator or {@code BoxItemSource}.
	 * This is not an operation-wide identity and may change after filtering.
	 */
	public int getLocalIndex() {
		return localIndex;
	}

	public int getGlobalIndex() {
		return globalIndex;
	}

	public void setGlobalIndex(int globalIndex) {
		if(globalIndex < 0) {
			throw new IllegalArgumentException("Expected a non-negative global index");
		}
		if(this.globalIndex != -1 && this.globalIndex != globalIndex) {
			throw new IllegalStateException("Global index is immutable once assigned");
		}
		this.globalIndex = globalIndex;
	}

	public long getVolume() {
		return count * box.getVolume();
	}

	public long getWeight() {
		return (long)count * box.getWeight();
	}

	public void setCount(int count) {
		this.count = count;
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

	public void setGroup(BoxItemGroup group) {
		this.group = group;
	}

	public BoxItemGroup getGroup() {
		return group;
	}
	
	public boolean isMaxLoad() {
		return box.isMaxLoad();		
	}

}
