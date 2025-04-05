package com.github.skjolber.packing.api.packager;

import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;

/**
 * 
 * A stackable item which fit within certain bounds, i.e. load dimensions of a container.
 * 
 */

public class MutableStackableItem extends BoxItem {

	private static final long serialVersionUID = 1L;
	
	protected final BoundedStackValue[] values;
	protected final boolean[] enabled;
	protected final BoxItem stackableItem;
	protected final Box stackable;
	
	protected final BoxStackValue minimumArea;
	protected final BoxStackValue maximumArea;
	
	protected int constrainedCount;

	public MutableStackableItem(BoxItem stackableItem, BoundedStackValue[] stackValues) {
		super(stackableItem.getStackable(), stackableItem.getCount());
		
		this.values = stackValues;
		this.stackableItem = stackableItem;
		this.stackable = stackableItem.getStackable();
		this.enabled = new boolean[stackValues.length];
		for(int i = 0; i < enabled.length; i++) {
			this.enabled[i] = true;
		}
		constrainedCount = stackableItem.getCount();
		
		this.minimumArea = Box.getMinimumArea(stackValues);
		this.maximumArea = Box.getMinimumArea(stackValues);
	}

	public MutableStackableItem(BoxItem stackableItem, List<BoundedStackValue> stackValues) {
		this(stackableItem, stackValues.toArray(new BoundedStackValue[stackValues.size()]));
	}

	public Box getStackable() {
		return stackable;
	}

	public BoundedStackValue getStackValue(int index) {
		return values[index];
	}

	public BoundedStackValue[] getStackValues() {
		return values;
	}
	
	public void setEnabled(int index, boolean value) {
		this.enabled[index] = value;
	}

	public void enable(int index) {
		this.enabled[index] = true;
	}

	public void disable(int index) {
		this.enabled[index] = false;
	}

	public void reset() {
		for(int i = 0; i < enabled.length; i++) {
			this.enabled[i] = true;
		}
		constrainedCount = stackableItem.getCount();
	}
}
