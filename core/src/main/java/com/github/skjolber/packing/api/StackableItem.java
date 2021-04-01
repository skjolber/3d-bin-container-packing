package com.github.skjolber.packing.api;

import java.io.Serializable;

/**
 * A {@linkplain Box} repeated one or more times. Typically corresponding to an order-line, but
 * can also represent multiple products which share the same size. 
 * 
 */
public class StackableItem implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final int count;
	private final Stackable box;

	public StackableItem(Stackable box) {
		this(box, 1);
	}

	public StackableItem(Stackable box, int count) {
		super();
		this.box = box;
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
}
