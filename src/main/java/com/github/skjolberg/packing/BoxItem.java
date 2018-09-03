package com.github.skjolberg.packing;

/**
 * A {@linkplain Box} repeated one or more times. Typically corresponding to an order-line, but
 * can also represent multiple products which share the same size. 
 * 
 */

public class BoxItem {

	private final int count;
	
	private final Box box;

	public BoxItem(final Box box) {
		this(box, 1);
	}

	public BoxItem(final Box box, final int count) {
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

}
