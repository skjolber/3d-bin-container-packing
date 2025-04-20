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
	
	public int resetCount; 

	public BoxItem(Box box) {
		this(box, 1);
	}

	public BoxItem(Box stackable, int count) {
		super();
		this.box = stackable;
		this.count = count;
		
		this.resetCount = count;
	}
	
	public BoxItem(Box stackable, int count, int index) {
		super();
		this.box = stackable;
		this.count = count;
		this.index = index;
		
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
		return String.format("%dx%s", count, box);
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
		return new BoxItem(box, count, index);
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
	public long getVolume() {
		return count * box.getVolume();
	}

	public long getWeight() {
		return count * box.getWeight();
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

}
