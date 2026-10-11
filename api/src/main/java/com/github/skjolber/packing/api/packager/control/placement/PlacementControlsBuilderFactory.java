package com.github.skjolber.packing.api.packager.control.placement;

/**
 * Creates the {@link PlacementControlsBuilder} which chooses the next box, orientation and position within a container.
 * <p>
 * <b>Thread-safety:</b> packagers run concurrently, for example with a parallel container packing strategy or a parallel
 * brute-force packager, so implementations must be safe for concurrent use; stateless implementations are. Return a new
 * builder (and controls) for each call.
 */
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
