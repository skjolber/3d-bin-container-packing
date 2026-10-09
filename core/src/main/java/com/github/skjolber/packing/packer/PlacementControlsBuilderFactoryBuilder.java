package com.github.skjolber.packing.packer;

import java.util.function.Consumer;

import com.github.skjolber.packing.api.packager.BoxItemComparator;
import com.github.skjolber.packing.api.packager.control.placement.PlacementComparator;
import com.github.skjolber.packing.api.packager.control.placement.PlacementComparatorFactory;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.comparator.LargestAreaBoxItemComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.comparator.placement.DefaultPlacementComparatorFactory;

/**
 * Configures the default placement controls of the plain and LAFF packagers: the support options, the box item comparator and
 * the placement ranking. Used by the packager builders' {@code withPlacementControlsBuilderFactory(Consumer)} (and the LAFF
 * builders' {@code withFirstPlacementControlsBuilderFactory(Consumer)}), and for the controls which the builders create when
 * none are set, so that the options have the same effect in both cases.
 * <p>
 * Unless a placement comparator is set, boxes and positions are ranked by the packager's default ranking (see
 * {@link #build()} and {@link #buildFirst()}): by support first if support is calculated and not required (full support
 * leaves no choice to rank), then by the dimensions of the default ranking. A placement comparator which is set replaces
 * the default ranking, so rank by support yourself then ({@code higherSupportIsBetter()}).
 */
public class PlacementControlsBuilderFactoryBuilder {

	private boolean requireFullSupport;
	private boolean calculateSupport;
	private BoxItemComparator boxItemComparator;
	private PlacementComparatorFactory comparatorFactory;

	/**
	 * @param calculateSupport whether to calculate the support of placements
	 * @return this builder
	 */
	public PlacementControlsBuilderFactoryBuilder withCalculateSupport(boolean calculateSupport) {
		this.calculateSupport = calculateSupport;
		return this;
	}

	/**
	 * @param require whether to place boxes only where they have full support
	 * @return this builder
	 */
	public PlacementControlsBuilderFactoryBuilder withRequireFullSupport(boolean require) {
		this.requireFullSupport = require;
		return this;
	}

	/**
	 * @param boxItemComparator the comparator which ranks the box items, in place of the default
	 * @return this builder
	 */
	public PlacementControlsBuilderFactoryBuilder withBoxItemComparator(BoxItemComparator boxItemComparator) {
		this.boxItemComparator = boxItemComparator;
		return this;
	}

	/**
	 * Wraps a fixed {@link PlacementComparator} via {@link PlacementComparatorFactory#of}
	 * so it is used as-is for every packing run, ignoring any disabled attributes.
	 *
	 * @param placementComparator the placement comparator
	 * @return this builder
	 */
	public PlacementControlsBuilderFactoryBuilder withPlacementComparator(PlacementComparator placementComparator) {
		this.comparatorFactory = PlacementComparatorFactory.of(placementComparator);
		return this;
	}

	/**
	 * Configures a {@link DefaultPlacementComparatorFactory.Builder} via a consumer.
	 * The factory is used dynamically — per-run, only constraint dimensions that
	 * are active for that run are included. Position dimensions added via the
	 * consumer are always included.
	 * <p>
	 * Not an overload of {@link #withPlacementComparatorFactory(PlacementComparatorFactory)}, as an implicit lambda such as
	 * {@code ranking -> ranking.lowerZIsBetter()} would be ambiguous between the two.
	 *
	 * @param consumer configures the factory
	 * @return this builder
	 */
	public PlacementControlsBuilderFactoryBuilder withPlacementComparators(Consumer<DefaultPlacementComparatorFactory.Builder> consumer) {
		DefaultPlacementComparatorFactory.Builder f = DefaultPlacementComparatorFactory.newFactory();
		consumer.accept(f);
		this.comparatorFactory = f;
		return this;
	}

	/**
	 * Sets a pre-configured {@link PlacementComparatorFactory} directly.
	 *
	 * @param factory the factory
	 * @return this builder
	 */
	public PlacementControlsBuilderFactoryBuilder withPlacementComparatorFactory(PlacementComparatorFactory factory) {
		this.comparatorFactory = factory;
		return this;
	}

	/**
	 * Create the placement controls which choose among all remaining boxes. By default the box with the highest volume, then
	 * the highest weight, is placed first, at the position with the lowest area, then the lowest z.
	 *
	 * @return the placement controls factory
	 */
	public PlacementControlsBuilderFactory build() {
		return build(VolumeThenWeightBoxItemComparator.getInstance(), ranking -> ranking.higherVolumeIsBetter()
				.higherWeightIsBetter()
				.lowerAreaIsBetter()
				.lowerZIsBetter());
	}

	/**
	 * Create the placement controls for the first placement of a LAFF level. By default the box with the largest area is placed
	 * first, at the lowest z.
	 *
	 * @return the placement controls factory
	 */
	public PlacementControlsBuilderFactory buildFirst() {
		return build(new LargestAreaBoxItemComparator(), ranking -> ranking.lowerZIsBetter()
				.higherAreaIsBetter()
				.higherVolumeIsBetter()
				.higherWeightIsBetter());
	}

	private PlacementControlsBuilderFactory build(BoxItemComparator defaultBoxItemComparator, Consumer<DefaultPlacementComparatorFactory.Builder> defaultRanking) {
		BoxItemComparator boxItemComparator = this.boxItemComparator != null ? this.boxItemComparator : defaultBoxItemComparator;

		PlacementComparatorFactory factory = comparatorFactory;
		if(factory == null) {
			DefaultPlacementComparatorFactory.Builder ranking = DefaultPlacementComparatorFactory.newFactory();
			if(!requireFullSupport && calculateSupport) {
				ranking.higherSupportIsBetter();
			}
			defaultRanking.accept(ranking);
			factory = ranking.compile();
		} else if(factory instanceof DefaultPlacementComparatorFactory.Builder ranking) {
			// compile once, instead of for every container
			factory = ranking.compile();
		}
		return new LoadAwarePlacementControlsBuilderFactory(factory, boxItemComparator, calculateSupport, requireFullSupport);
	}
}
