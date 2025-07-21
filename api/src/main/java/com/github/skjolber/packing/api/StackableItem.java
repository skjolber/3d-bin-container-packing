package com.github.skjolber.packing.api;

import java.io.Serializable;

/**
 * A {@linkplain Stackable} repeated one or more times. Typically corresponding to an order-line, but
 * can also represent multiple products which share the same size.
 * 
 */
public class StackableItem implements Serializable {

	private static final long serialVersionUID = 1L;

	protected int count;
	protected final Stackable stackable;

	public StackableItem(Stackable box) {
		this(box, 1);
	}

	public StackableItem(Stackable stackable, int count) {
		super();
		this.stackable = stackable;
		this.count = count;
	}

	public int getCount() {
		return count;
	}

	public Stackable getStackable() {
		return stackable;
	}

	@Override
	public String toString() {
		return String.format("%dx%s", count, stackable);
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

	public StackableItem clone() {
		return new StackableItem(stackable, count);
	}

}
