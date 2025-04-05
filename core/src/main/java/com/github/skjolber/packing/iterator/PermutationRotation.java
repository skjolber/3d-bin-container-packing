package com.github.skjolber.packing.iterator;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;

/**
 * 
 * A {@linkplain Stackable} in a specific {@linkplain StackValue}.
 *
 */

public class PermutationRotation {

	private final Box stackable;
	private final BoxStackValue value;

	public PermutationRotation(Box stackable, BoxStackValue value) {
		super();
		this.stackable = stackable;
		this.value = value;
	}

	public Box getStackable() {
		return stackable;
	}

	public BoxStackValue getValue() {
		return value;
	}

}
