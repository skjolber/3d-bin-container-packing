package com.github.skjolber.packing.impl;

import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.Placement;

public class ValidatingStack extends Stack {

	public void add(Placement e) {
		for (Placement stackPlacement : entries) {
			if(stackPlacement.intersects(e)) {
				throw new IllegalArgumentException(e + " intersects " + stackPlacement);
			}
		}
		super.add(e);
	}

}
