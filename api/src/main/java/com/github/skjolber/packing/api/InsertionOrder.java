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
		switch (access) {
			case TOP:
				// other is above
				return other.getAbsoluteZ() > placement.getAbsoluteEndZ() && overlapsXY(placement, other);
			case FRONT:
				// other is between placement and the door
				return other.getAbsoluteX() > placement.getAbsoluteEndX() && overlapsYZ(placement, other);
			default:
				return false;
		}
	}

	private static boolean overlapsXY(Placement a, Placement b) {
		return a.getAbsoluteX() <= b.getAbsoluteEndX() && b.getAbsoluteX() <= a.getAbsoluteEndX()
				&& a.getAbsoluteY() <= b.getAbsoluteEndY() && b.getAbsoluteY() <= a.getAbsoluteEndY();
	}

	private static boolean overlapsYZ(Placement a, Placement b) {
		return a.getAbsoluteY() <= b.getAbsoluteEndY() && b.getAbsoluteY() <= a.getAbsoluteEndY()
				&& a.getAbsoluteZ() <= b.getAbsoluteEndZ() && b.getAbsoluteZ() <= a.getAbsoluteEndZ();
	}
}
