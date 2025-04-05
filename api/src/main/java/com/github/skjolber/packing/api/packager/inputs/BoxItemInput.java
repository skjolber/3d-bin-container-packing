package com.github.skjolber.packing.api.packager.inputs;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.BoxItem;

public interface StackableItemInput {
	
	int getIndex();
	BoxItem getStackableItem();

	int getSize();
	
	StackValue get(int index);
	
	int getCount();
	boolean decrementCount(int amount);
}
