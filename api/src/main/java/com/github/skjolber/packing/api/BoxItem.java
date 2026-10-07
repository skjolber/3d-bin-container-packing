package com.github.skjolber.packing.api;

import java.io.Serializable;

/**
 * A {@linkplain Box} repeated one or more times. Typically corresponding to an
 * order-line, but can also represent multiple products which share the same
 * size.
 * <p>
 * Box items are the input of packaging: packing does not modify them, so they can be packed again, also concurrently.
 * The placements of results refer to them (see {@link Placement#getBoxItem()}).
 */

public class BoxItem implements Serializable {

	private static final long serialVersionUID = 1L;

	protected final int count;
	protected final Box box;

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
	}

	public int getCount() {
		return count;
	}

	public Box getBox() {
		return box;
	}

	@Override
	public String toString() {
		return String.format("%dx%s", count, box);
	}

	/**
	 * @return a box item with the same box, count, container priority and extraction order, in no group
	 */
	public BoxItem copy() {
		return new BoxItem(box, count).withOrderingOf(this);
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

	public int getContainerPriority() {
		return containerPriority;
	}

	public int getExtractionOrder() {
		return extractionOrder;
	}

	/**
	 * @return the volume of all the boxes
	 */
	public long getVolume() {
		return count * box.getVolume();
	}

	/**
	 * @return the weight of all the boxes
	 */
	public long getWeight() {
		return (long)count * box.getWeight();
	}

	/** Set by the group's constructor */
	void setGroup(BoxItemGroup group) {
		this.group = group;
	}

	/**
	 * @return the group which this box item was last added to, or null
	 */
	public BoxItemGroup getGroup() {
		return group;
	}

	public boolean isMaxLoad() {
		return box.isMaxLoad();
	}

}
