package com.github.skjolber.packing;

import java.io.Serializable;

/**
 * A {@linkplain Box} repeated one or more times. Typically corresponding to an order-line, but
 * can also represent multiple products which share the same size. 
 * 
 */
public class BoxItem implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final int count;
	private final Box box;

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
}
