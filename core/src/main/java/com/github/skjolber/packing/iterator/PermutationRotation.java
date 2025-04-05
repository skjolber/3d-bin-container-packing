package com.github.skjolber.packing.iterator;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;

/**
 * 
 * A {@linkplain Box} in a specific {@linkplain BoxStackValue}.
 *
 */

public class PermutationRotation {

	private final Box box;
	private final BoxStackValue value;

	public PermutationRotation(Box stackable, BoxStackValue value) {
		super();
		this.box = stackable;
		this.value = value;
	}

	public Box getBox() {
		return box;
	}

	public BoxStackValue getBoxStackValue() {
		return value;
	}

}
