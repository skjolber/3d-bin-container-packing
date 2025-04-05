package com.github.skjolber.packing.api;

import java.io.Serializable;

/**
 * A {@linkplain Box} repeated one or more times. Typically corresponding to an order-line, but
 * can also represent multiple products which share the same size.
 * 
 */
public class BoxItem implements Serializable {

	private static final long serialVersionUID = 1L;

	protected int count;
	protected final Box box;
	protected int index = -1;

	public BoxItem(Box box) {
		this(box, 1);
	}

	public BoxItem(Box stackable, int count) {
		super();
		this.box = stackable;
		this.count = count;
	}
	
	public BoxItem(Box stackable, int count, int index) {
		super();
		this.box = stackable;
		this.count = count;
		this.index = index;
	}

	public int getCount() {
		return count;
	}

	public Box getStackable() {
		return box;
	}

	@Override
	public String toString() {
		return String.format("%dx%s", count, box);
	}
	
	public void decrement() {
		count--;
	}

	public boolean isEmpty() {
		return count == 0;
	}
	
	public boolean decrement(int value) {
		this.count = this.count - value;
		return count > 0;
	}

	public BoxItem clone() {
		return new BoxItem(box, count, index);
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}

}
