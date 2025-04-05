package com.github.skjolber.packing.impl;

import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;

public class ValidatingStack extends Stack {

	public void add(StackPlacement e) {
		for (StackPlacement stackPlacement : entries) {
			if(stackPlacement.intersects(e)) {
				throw new IllegalArgumentException(e + " intersects " + stackPlacement);
			}
		}
		super.add(e);
	}

}
