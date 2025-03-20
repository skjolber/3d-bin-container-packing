package com.github.skjolber.packing.api;

import java.io.Serializable;

/**
 * A {@linkplain Stackable} repeated one or more times. Typically corresponding to an order-line, but
 * can also represent multiple products which share the same size.
 * 
 */
public class BoxItem implements Serializable {

	private static final long serialVersionUID = 1L;

	protected int count;
	protected final Box box;

	public BoxItem(Box box) {
		this(box, 1);
	}

	public BoxItem(Box stackable, int count) {
		super();
		this.box = stackable;
		this.count = count;
	}

	public int getCount() {
		return count;
	}

	public Stackable getStackable() {
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
	
	public void decrement(int value) {
		this.count = this.count - value;
	}

	public BoxItem clone() {
		return new BoxItem(box, count);
	}

}
