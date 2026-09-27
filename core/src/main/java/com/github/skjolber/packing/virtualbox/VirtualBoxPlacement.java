package com.github.skjolber.packing.virtualbox;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;

/**
 * One original physical box at fixed coordinates relative to a virtual box.
 * Generators validate the inventory once; construction does not rescan permitted rotations.
 */
public class VirtualBoxPlacement {
	protected final BoxItem item;
	protected final BoxStackValue stackValue;
	protected final int x;
	protected final int y;
	protected final int z;

	public VirtualBoxPlacement(BoxItem item, BoxStackValue stackValue, int x, int y, int z) {
		this.item = item;
		this.stackValue = stackValue;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public BoxItem item() { return item; }
	public BoxStackValue stackValue() { return stackValue; }
	public int x() { return x; }
	public int y() { return y; }
	public int z() { return z; }
}
