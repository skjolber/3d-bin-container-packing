package com.github.skjolber.packing.api;

import com.github.skjolber.packing.api.ep.Point;

public interface PlacementOperation {

	// handle: add or subtract
	boolean canHandle(Stack stack, Point point, Box box);
	
}
