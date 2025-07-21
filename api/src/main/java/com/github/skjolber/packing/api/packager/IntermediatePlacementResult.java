package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.ep.Point;

public class IntermediatePlacementResult {

	private BoxStackValue stackValue;
	private Point point;
	private int index;

	public IntermediatePlacementResult(int index, BoxStackValue stackValue, Point point) {
		super();
		this.index = index;
		this.stackValue = stackValue;
		this.point = point;
	}
	
	public BoxItem getBoxItem() {
		return stackValue.getBox().getBoxItem();
	}
	
	public Point getPoint() {
		return point;
	}
	
	public BoxStackValue getStackValue() {
		return stackValue;
	}
	
	public int getIndex() {
		return index;
	}
}