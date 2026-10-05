package com.github.skjolber.packing.api;

/**
 * The rules for the order in which boxes are inserted into a container (the order of the placements of a stack).
 * A box must be inserted after
 * <ul>
 * <li>the boxes it rests on (its top touches their bottom and their footprints overlap), and</li>
 * <li>for {@link ContainerAccess#TOP}, the boxes below it which it would otherwise have to pass, and for
 * {@link ContainerAccess#FRONT}, the boxes between it and the door which it would otherwise have to pass.</li>
 * </ul>
 * With these rules, loads only grow while loading, so a stack whose final loads are within the limits is within the
 * limits at every step of loading, and of unloading (which removes boxes with nothing on them).
 */
public final class InsertionOrder {

	private InsertionOrder() {
	}

	/**
	 * @return true if {@code placement} rests on {@code supporter}
	 */
	public static boolean restsOn(Placement placement, Placement supporter) {
		return supporter.getAbsoluteEndZ() + 1 == placement.getAbsoluteZ() && overlapsXY(placement, supporter);
	}

	/**
	 * @return true if {@code first} must be inserted before {@code second}, because {@code second} rests on it,
	 *         or because it could not be inserted after {@code second} with the given access
	 */
	public static boolean mustPrecede(Placement first, Placement second, ContainerAccess access) {
		if(restsOn(second, first)) {
			return true;
		}
		return isBlockedBy(first, second, access);
	}

	/**
	 * @return true if inserting {@code placement} would pass through {@code other} (were it already there), with the
	 *         given access; always false for {@link ContainerAccess#ANY}
	 */
	public static boolean isBlockedBy(Placement placement, Placement other, ContainerAccess access) {
		return isBlockedBy(placement.getAbsoluteX(), placement.getAbsoluteY(), placement.getAbsoluteZ(),
				placement.getAbsoluteEndX(), placement.getAbsoluteEndY(), placement.getAbsoluteEndZ(), other, access);
	}

	/**
	 * @return true if a box at the given coordinates (inclusive) must be inserted before {@code other}, see
	 *         {@link #mustPrecede(Placement, Placement, ContainerAccess)}
	 */
	public static boolean mustPrecede(int x, int y, int z, int endX, int endY, int endZ, Placement other, ContainerAccess access) {
		if(endZ + 1 == other.getAbsoluteZ() && overlapsXY(x, y, endX, endY, other)) {
			// other rests on it
			return true;
		}
		return isBlockedBy(x, y, z, endX, endY, endZ, other, access);
	}

	private static boolean isBlockedBy(int x, int y, int z, int endX, int endY, int endZ, Placement other, ContainerAccess access) {
		switch (access) {
			case TOP:
				// other is above
				return other.getAbsoluteZ() > endZ && overlapsXY(x, y, endX, endY, other);
			case FRONT:
				// other is between the box and the door
				return other.getAbsoluteX() > endX
						&& y <= other.getAbsoluteEndY() && other.getAbsoluteY() <= endY
						&& z <= other.getAbsoluteEndZ() && other.getAbsoluteZ() <= endZ;
			default:
				return false;
		}
	}

	private static boolean overlapsXY(int x, int y, int endX, int endY, Placement other) {
		return x <= other.getAbsoluteEndX() && other.getAbsoluteX() <= endX
				&& y <= other.getAbsoluteEndY() && other.getAbsoluteY() <= endY;
	}

	private static boolean overlapsXY(Placement a, Placement b) {
		return a.getAbsoluteX() <= b.getAbsoluteEndX() && b.getAbsoluteX() <= a.getAbsoluteEndX()
				&& a.getAbsoluteY() <= b.getAbsoluteEndY() && b.getAbsoluteY() <= a.getAbsoluteEndY();
	}

}
