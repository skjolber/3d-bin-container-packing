package com.github.skjolber.packing.api.packager.control.placement;

import com.github.skjolber.packing.api.Placement;

public interface PlacementControls<R extends Placement> {

	/**
	 * Get the next placement.
	 * 
	 * @param offset start offset
	 * @param length length
	 * @return the next placement, or null if not available.
	 */
	
	R getPlacement(int offset, int length);
	
}
