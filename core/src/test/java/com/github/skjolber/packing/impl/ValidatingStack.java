package com.github.skjolber.packing.impl;

import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.StackPlacement;

public class ValidatingStack extends DefaultStack {

	public void add(StackPlacement e) {
		for(StackPlacement stackPlacement : entries) {
			if(stackPlacement.intersects(e)) {
				throw new IllegalArgumentException(e + " intersects " + stackPlacement);
			}
		}
		super.add(e);
	}

	
}
