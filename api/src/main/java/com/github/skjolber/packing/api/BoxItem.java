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

	public BoxItem(Box box) {
		this(box, 1);
	}

	public BoxItem(Box box, int count) {
		super();
		this.box = box;
		this.count = count;

		this.resetCount = count;
		box.setBoxItem(this);
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
		box.setBoxItem(this);
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

	public BoxItem clone() {
		return new BoxItem(box, count, localIndex, globalIndex);
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
