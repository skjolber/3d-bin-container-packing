package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxStackValue;

public class BoundedStackValue extends BoxStackValue {

	protected final int index;
	
	public BoundedStackValue(BoxStackValue values, int index) {
		super(values);
		this.index = index;
	}

	public int getIndex() {
		return index;
	}
	
}
