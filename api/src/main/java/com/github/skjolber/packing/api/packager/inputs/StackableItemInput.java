package com.github.skjolber.packing.api.packager.inputs;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.StackableItem;

public interface StackableItemInput {
	
	int getIndex();
	StackableItem getStackableItem();

	int getSize();
	
	StackValue get(int index);
	
	int getCount();
	boolean decrementCount(int amount);
}
