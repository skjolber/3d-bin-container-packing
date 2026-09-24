package com.github.skjolber.packing.comparator.placement;

import java.util.Set;

/**
 * Identifies one comparison dimension and its preferred direction.
 *
 * <p>Constants cover the full set of supported dimensions:
 * <ul>
 *   <li><b>Position</b> — x, y, z coordinates (never skipped; always present)</li>
 *   <li><b>Size</b> — footprint area, volume, box weight, support ratio (never skipped)</li>
 *   <li><b>Load constraints</b> — max-load weight, pressure, box count, identical-only
 *       restriction (skipped when the corresponding constraint is not active)</li>
 * </ul>
 *
 * <p>Constraint constants use the IDs {@code "weight"}, {@code "pressure"}, {@code "count"},
 * and {@code "identical"} for filtering via
 * {@link #isSkippable(Set)}. Position / size constants always return {@code false} from
 * {@link #isSkippable}.
 */
public interface PlacementComparatorAttribute {

	// =========================================================================
	// Attribute ID constants
	// =========================================================================

	String ID_X        = "x";
	String ID_Y        = "y";
	String ID_Z        = "z";
	String ID_AREA     = "area";
	String ID_VOLUME   = "volume";
	String ID_BOX_WEIGHT = "box_weight";
	String ID_SUPPORT  = "support";
	String ID_WEIGHT   = "weight";
	String ID_PRESSURE = "pressure";
	String ID_COUNT    = "count";
	String ID_IDENTICAL = "identical";

	// =========================================================================
	// Position — X
	// =========================================================================

	/** Prefer placements with a <b>lower x</b> coordinate. */
	PlacementComparatorAttribute LOWER_X = positional(ID_X, false);
	/** Prefer placements with a <b>higher x</b> coordinate. */
	PlacementComparatorAttribute HIGHER_X = positional(ID_X, true);

	// =========================================================================
	// Position — Y
	// =========================================================================

	/** Prefer placements with a <b>lower y</b> coordinate. */
	PlacementComparatorAttribute LOWER_Y = positional(ID_Y, false);
	/** Prefer placements with a <b>higher y</b> coordinate. */
	PlacementComparatorAttribute HIGHER_Y = positional(ID_Y, true);

	// =========================================================================
	// Position — Z
	// =========================================================================

	/** Prefer placements with a <b>lower z</b> coordinate (floor-first). */
	PlacementComparatorAttribute LOWER_Z = positional(ID_Z, false);
	/** Prefer placements with a <b>higher z</b> coordinate. */
	PlacementComparatorAttribute HIGHER_Z = positional(ID_Z, true);

	// =========================================================================
	// Footprint area  (dx × dy)
	// =========================================================================

	/** Prefer placements where the box has a <b>larger footprint area</b>. */
	PlacementComparatorAttribute HIGHER_AREA = positional(ID_AREA, true);
	/** Prefer placements where the box has a <b>smaller footprint area</b>. */
	PlacementComparatorAttribute LOWER_AREA = positional(ID_AREA, false);

	// =========================================================================
	// Volume  (dx × dy × dz)
	// =========================================================================

	/** Prefer placements where the box has a <b>larger volume</b>. */
	PlacementComparatorAttribute HIGHER_VOLUME = positional(ID_VOLUME, true);
	/** Prefer placements where the box has a <b>smaller volume</b>. */
	PlacementComparatorAttribute LOWER_VOLUME = positional(ID_VOLUME, false);

	// =========================================================================
	// Box weight (physical weight of the box)
	// =========================================================================

	/** Prefer placements where the box is <b>heavier</b>. */
	PlacementComparatorAttribute HIGHER_WEIGHT = positional(ID_BOX_WEIGHT, true);
	/** Prefer placements where the box is <b>lighter</b>. */
	PlacementComparatorAttribute LOWER_WEIGHT = positional(ID_BOX_WEIGHT, false);

	// =========================================================================
	// Support ratio  (supportedArea / (dx × dy))
	// =========================================================================

	/** Prefer placements with a <b>higher support ratio</b> (more area supported from below). */
	PlacementComparatorAttribute HIGHER_SUPPORT = positional(ID_SUPPORT, true);
	/** Prefer placements with a <b>lower support ratio</b>. */
	PlacementComparatorAttribute LOWER_SUPPORT = positional(ID_SUPPORT, false);

	// =========================================================================
	// Max-load weight  (load constraint — skipped when weight constraint inactive)
	// =========================================================================

	/** Prefer placements where the box allows a <b>higher max-load weight</b> on top. */
	PlacementComparatorAttribute HIGHER_MAX_LOAD_WEIGHT = constraint(ID_WEIGHT, true, true);
	/** Prefer placements where the box allows a <b>lower max-load weight</b> on top. */
	PlacementComparatorAttribute LOWER_MAX_LOAD_WEIGHT = constraint(ID_WEIGHT, false, true);

	// =========================================================================
	// Max-load pressure  (load constraint — skipped when pressure constraint inactive)
	// =========================================================================

	/** Prefer placements where the box tolerates a <b>higher max-load pressure</b> on top. */
	PlacementComparatorAttribute HIGHER_MAX_LOAD_PRESSURE = constraint(ID_PRESSURE, true, true);
	/** Prefer placements where the box tolerates a <b>lower max-load pressure</b> on top. */
	PlacementComparatorAttribute LOWER_MAX_LOAD_PRESSURE = constraint(ID_PRESSURE, false, true);

	// =========================================================================
	// Max-load box count  (load constraint — skipped when count constraint inactive)
	// =========================================================================

	/** Prefer placements where the box allows a <b>higher number of boxes</b> stacked on top. */
	PlacementComparatorAttribute HIGHER_MAX_LOAD_BOX_COUNT = constraint(ID_COUNT, true, true);
	/** Prefer placements where the box allows a <b>lower number of boxes</b> on top. */
	PlacementComparatorAttribute LOWER_MAX_LOAD_BOX_COUNT = constraint(ID_COUNT, false, true);

	// =========================================================================
	// Identical-only restriction  (load constraint — skipped when identical constraint inactive)
	// =========================================================================

	/**
	 * Prefer placements where the box has <b>no identical-only restriction</b>.
	 * The <em>absence</em> of the restriction is better (leaves more future stacking options).
	 */
	PlacementComparatorAttribute NO_IDENTICAL_CONSTRAINT = constraint(ID_IDENTICAL, false, false);

	/**
	 * Prefer placements where the box <b>has</b> the identical-only restriction.
	 * The <em>presence</em> of the restriction is better.
	 */
	PlacementComparatorAttribute IDENTICAL_CONSTRAINT = constraint(ID_IDENTICAL, false, true);

	// =========================================================================
	// Interface methods
	// =========================================================================

	/** Returns the attribute ID, used for filtering via {@link #isSkippable(Set)}. */
	String getId();

	/** Returns {@code true} when the presence of this attribute value is preferred. */
	boolean presenceIsBetter();

	/** Returns {@code true} when the absence of this attribute value is preferred. */
	boolean absenceIsBetter();

	/** Returns {@code true} when a higher numeric value for this attribute is preferred. */
	boolean higherIsBetter();

	/** Returns {@code true} when a lower numeric value for this attribute is preferred. */
	boolean lowerIsBetter();

	/**
	 * Returns {@code true} when this dimension should be skipped given the set of active
	 * constraint IDs. Position / size attributes always return {@code false} (never skipped).
	 * Constraint attributes return {@code true} when their ID is not in {@code available}.
	 *
	 * @param available set of active constraint IDs (e.g. {@code "weight"}, {@code "pressure"})
	 * @return {@code true} to skip this dimension
	 */
	boolean isSkippable(Set<String> available);

	// =========================================================================
	// Private static factories
	// =========================================================================

	/** Creates a position / size attribute that is never skipped. */
	private static PlacementComparatorAttribute positional(String id, boolean higherIsBetter) {
		return new DefaultPlacementComparatorAttribute(id, higherIsBetter, !higherIsBetter) {
			@Override
			public boolean isSkippable(Set<String> available) {
				return false;
			}
		};
	}

	/**
	 * Creates a load-constraint attribute that is skipped when its ID is absent from the
	 * available set.
	 *
	 * @param id              constraint ID used for filtering (e.g. {@code "weight"})
	 * @param higherIsBetter  {@code true} when a higher value is preferred
	 * @param presenceIsBetter {@code true} when presence of the constraint is preferred
	 */
	private static PlacementComparatorAttribute constraint(String id, boolean higherIsBetter,
			boolean presenceIsBetter) {
		return new DefaultPlacementComparatorAttribute(id, higherIsBetter, presenceIsBetter);
	}
}

