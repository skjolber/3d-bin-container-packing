package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.StackableItem;

public interface StackValueCollection {

	int size();
	
	StackValue get(int index);

	void remove(int index, int count);
}
