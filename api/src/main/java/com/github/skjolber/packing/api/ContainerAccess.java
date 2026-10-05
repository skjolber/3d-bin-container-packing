package com.github.skjolber.packing.api;

/**
 * How boxes get into a container. The placements of a stack are in insertion order: each box is placed after the
 * boxes it rests on, and, depending on the access, its path from the opening must be free when it is inserted.
 */
public enum ContainerAccess {

	/** No restriction on the path; boxes are only placed after the boxes they rest on. */
	ANY,

	/** From above, for example a pallet or an open-top container: no box inserted before it is above it. */
	TOP,

	/**
	 * Through a door at the end of the x axis ({@code x = dx}), loading from {@code x = 0} towards the door: no box
	 * inserted before it is between it and the door.
	 */
	FRONT
}
