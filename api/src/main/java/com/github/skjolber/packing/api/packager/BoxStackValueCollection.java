package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxStackValue;

public interface BoxStackValueCollection {

	int size();
	
	BoxStackValue get(int index);

	void remove(int index, int count);
}
