package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.ep.Point;

public class IntermediatePlacementResult {

	private BoxItem boxItem; 
	private BoxStackValue stackValue;
	private Point point;

	public IntermediatePlacementResult(BoxItem boxItem, BoxStackValue stackValue, Point point) {
		super();
		this.boxItem = boxItem;
		this.stackValue = stackValue;
		this.point = point;
	}
	
	public BoxItem getBoxItem() {
		return boxItem;
	}
	
	public Point getPoint() {
		return point;
	}
	
	public BoxStackValue getStackValue() {
		return stackValue;
	}
}