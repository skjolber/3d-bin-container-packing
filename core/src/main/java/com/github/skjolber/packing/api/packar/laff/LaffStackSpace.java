package com.github.skjolber.packing.api.packar.laff;

import com.github.skjolber.packing.api.StackSpace;

public class LaffStackSpace extends StackSpace {

	protected StackSpace remainder;
	
	public void setRemainder(StackSpace remainder) {
		this.remainder = remainder;
	}
	
	public StackSpace getRemainder() {
		return remainder;
	}
}
