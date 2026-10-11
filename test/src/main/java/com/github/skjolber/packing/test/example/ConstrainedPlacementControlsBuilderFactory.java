package com.github.skjolber.packing.test.example;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilder;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;

/**
 * Creates {@linkplain ConstrainedPlacementControlsBuilder}s, with the settings which apply to the whole packager:
 * whether to require full support, and the regions where no box may be placed. The per-box rules are marked on the boxes,
 * see {@linkplain PlacementConstraint}.
 * <p>
 * The factory is immutable, so it can be shared by concurrent packaging operations (as the packagers require), and each
 * of the {@code with} methods returns a new factory:
 *
 * <pre>
 * PlainPackager.newBuilder()
 *     .withPlacementControlsBuilderFactory(new ConstrainedPlacementControlsBuilderFactory()
 *         .withRequireFullSupport(true)
 *         .withForbiddenRegion(1, 1, 0, 1, 1, 1))
 *     .build();
 * </pre>
 *
 * The packager's own {@code withRequireFullSupport(..)} configures the default placement controls and cannot be combined with
 * a placement controls factory, which is why the setting is on this factory.
 * <p>
 * Box load limits are not supported ({@linkplain #supportsLoad()} is false), so the packagers reject inputs which have them.
 */
public final class ConstrainedPlacementControlsBuilderFactory implements PlacementControlsBuilderFactory {

	private final boolean requireFullSupport;
	private final List<ForbiddenRegion> forbiddenRegions;

	public ConstrainedPlacementControlsBuilderFactory() {
		this(false, List.of());
	}

	private ConstrainedPlacementControlsBuilderFactory(boolean requireFullSupport, List<ForbiddenRegion> forbiddenRegions) {
		this.requireFullSupport = requireFullSupport;
		this.forbiddenRegions = forbiddenRegions;
	}

	/**
	 * @param requireFullSupport true to place a box only where its whole bottom face rests on the floor or on other boxes
	 * @return a new factory
	 */
	public ConstrainedPlacementControlsBuilderFactory withRequireFullSupport(boolean requireFullSupport) {
		return new ConstrainedPlacementControlsBuilderFactory(requireFullSupport, forbiddenRegions);
	}

	/**
	 * Add a region where no box may be placed, in each container's load coordinates.
	 *
	 * @param x the first x coordinate of the region
	 * @param y the first y coordinate of the region
	 * @param z the first z coordinate of the region
	 * @param dx the extent of the region in the x direction
	 * @param dy the extent of the region in the y direction
	 * @param dz the extent of the region in the z direction
	 * @return a new factory
	 */
	public ConstrainedPlacementControlsBuilderFactory withForbiddenRegion(int x, int y, int z, int dx, int dy, int dz) {
		if(dx <= 0 || dy <= 0 || dz <= 0) {
			throw new IllegalArgumentException("Expected a region with a positive size");
		}
		List<ForbiddenRegion> regions = new ArrayList<>(forbiddenRegions);
		regions.add(new ForbiddenRegion(x, y, z, dx, dy, dz));
		return new ConstrainedPlacementControlsBuilderFactory(requireFullSupport, List.copyOf(regions));
	}

	@Override
	public PlacementControlsBuilder createPlacementControlsBuilder() {
		// a new builder for each request: the packager populates it with the state of one packing attempt
		return new ConstrainedPlacementControlsBuilder(requireFullSupport, forbiddenRegions);
	}
}
