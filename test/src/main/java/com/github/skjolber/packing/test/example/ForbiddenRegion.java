package com.github.skjolber.packing.test.example;

import com.github.skjolber.packing.api.BoxStackValue;

/**
 * An axis-aligned region of a container, in the container's load coordinates, where no box may be placed.
 *
 * @param x the first x coordinate of the region
 * @param y the first y coordinate of the region
 * @param z the first z coordinate of the region
 * @param dx the extent of the region in the x direction
 * @param dy the extent of the region in the y direction
 * @param dz the extent of the region in the z direction
 */
public record ForbiddenRegion(int x, int y, int z, int dx, int dy, int dz) {

	/**
	 * @param boxX the first x coordinate of a box
	 * @param boxY the first y coordinate of a box
	 * @param boxZ the first z coordinate of a box
	 * @param stackValue the rotation of the box
	 * @return true if the box would take up any of the space of this region
	 */
	public boolean intersects(int boxX, int boxY, int boxZ, BoxStackValue stackValue) {
		// the box covers [boxX, boxX + dx), and the same goes for the region
		return boxX < x + dx && x < boxX + stackValue.getDx()
				&& boxY < y + dy && y < boxY + stackValue.getDy()
				&& boxZ < z + dz && z < boxZ + stackValue.getDz();
	}
}
