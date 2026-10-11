package com.github.skjolber.packing.api;

/**
 * How boxes get into a container, which decides the orders in which they can be inserted. The placements of a stack
 * are in insertion order: a box is inserted after
 * <ul>
 * <li>the boxes it rests on (its bottom touches their top, and their footprints overlap), and</li>
 * <li>for {@link #TOP}, the boxes below it which it would otherwise have to pass, and for {@link #FRONT}, the boxes
 * between it and the door which it would otherwise have to pass.</li>
 * </ul>
 * With these rules, loads only grow while loading, so a stack whose final loads are within the limits is within the
 * limits at every step of loading, and of unloading (which removes boxes with nothing on them).
 * <br>
 * <br>
 * See {@link Placement#restsOn(Placement)}, {@link Placement#isBlockedBy(Placement, ContainerAccess)} and
 * {@link Placement#mustPrecede(Placement, ContainerAccess)}.
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
