package com.github.skjolber.packing.iterator;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;

/**
 * 
 * A {@linkplain Box} in a specific {@linkplain BoxStackValue}.
 *
 */

public class PermutationRotation {

	private final BoxItem boxItem;
	private final BoxStackValue value;

	public PermutationRotation(BoxItem boxItem, BoxStackValue value) {
		super();
		this.boxItem = boxItem;
		this.value = value;
	}

	public BoxItem getBoxItem() {
		return boxItem;
	}

	public BoxStackValue getBoxStackValue() {
		return value;
	}

}
