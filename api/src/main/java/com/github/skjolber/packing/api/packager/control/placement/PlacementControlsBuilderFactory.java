package com.github.skjolber.packing.api.packager.control.placement;

@FunctionalInterface
public interface PlacementControlsBuilderFactory {

	PlacementControlsBuilder createPlacementControlsBuilder();

	/**
	 * Whether the placement controls respect box load limits (max load weight, pressure and box count, identical boxes
	 * only). Packagers reject inputs with load limits when they do not, instead of ignoring the limits.
	 *
	 * @return true if the placement controls respect box load limits
	 */
	default boolean supportsLoad() {
		return false;
	}
}
