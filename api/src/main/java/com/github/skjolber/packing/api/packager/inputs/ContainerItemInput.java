package com.github.skjolber.packing.api.packager.inputs;

import com.github.skjolber.packing.api.ContainerItem;

public interface ContainerItemInput {

	int getIndex();
	ContainerItem getContainerItem();
	
	int size();
	
	StackableItemInput get(int index);
	
	boolean remove(StackableItemInput input, int count);

	int getCount();
	boolean decrementCount(int amount);
}
