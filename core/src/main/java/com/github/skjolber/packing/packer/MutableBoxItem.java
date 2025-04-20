package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.BoxItem;

public class MutableBoxItem extends BoxItem {

	private static final long serialVersionUID = 1L;
	
	public int resetCount; 

	public MutableBoxItem(BoxItem loadableItem) {
		super(loadableItem.getBox(), loadableItem.getCount(), loadableItem.getIndex());
		
		this.resetCount = loadableItem.getCount();
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
	
}
