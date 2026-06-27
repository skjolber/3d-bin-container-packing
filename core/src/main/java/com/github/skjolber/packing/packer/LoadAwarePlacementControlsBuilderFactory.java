package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilder;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.comparator.placement.PlacementComparatorFactory;

/**
 * Factory that produces {@link LoadAwarePlacementControlsBuilder} instances.
 *
 * <p>A {@link PlacementComparatorFactory} is required; each
 * {@link LoadAwarePlacementControlsBuilder#build()} call produces a fresh comparator
 * containing only the constraint dimensions that are both factory-enabled and active for
 * that run, followed by any configured position dimensions.
 *
 * <p>To use a fixed {@link com.github.skjolber.packing.comparator.placement.PlacementComparator},
 * wrap it via {@link PlacementComparatorFactory#of}.
 */
public class LoadAwarePlacementControlsBuilderFactory implements PlacementControlsBuilderFactory {

	protected final PlacementComparatorFactory comparatorBuilderFactory;
	protected final Comparator<BoxItem> boxItemComparator;
	protected final boolean requireFullSupport;
	protected final boolean calculateSupport;

	/**
	 * Creates a factory with a dynamic comparator that adapts to active constraints.
	 *
	 * <p>The {@link PlacementComparatorFactory} is queried each time
	 * {@link LoadAwarePlacementControlsBuilder#build()} is called, receiving the active
	 * constraint flags. The produced comparator includes only the dimensions that are both
	 * factory-enabled and active for the current packing run.
	 *
	 * @param comparatorBuilderFactory factory that creates comparators from active flags; must not be {@code null}
	 * @param boxItemComparator        comparator for ordering box items
	 * @param calculateSupport         whether to calculate placement support
	 * @param requireFullSupport       whether full placement support is required
	 */
	public LoadAwarePlacementControlsBuilderFactory(
			PlacementComparatorFactory comparatorBuilderFactory,
			Comparator<BoxItem> boxItemComparator,
			boolean calculateSupport,
			boolean requireFullSupport) {
		this.comparatorBuilderFactory = comparatorBuilderFactory;
		this.boxItemComparator        = boxItemComparator;
		this.requireFullSupport       = requireFullSupport;
		this.calculateSupport         = calculateSupport;
	}

	@Override
	public PlacementControlsBuilder createPlacementControlsBuilder() {
		return new LoadAwarePlacementControlsBuilder()
				.withPlacementComparatorBuilderFactory(comparatorBuilderFactory)
				.withBoxItemComparator(boxItemComparator)
				.withStability(calculateSupport, requireFullSupport);
	}
}
