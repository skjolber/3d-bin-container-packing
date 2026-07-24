package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilder;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;
import com.github.skjolber.packing.comparator.placement.PlacementComparatorAttribute;
import com.github.skjolber.packing.comparator.placement.PlacementComparatorFactory;

/**
 * Builds a {@link PlacementControls} instance for a single packing run.
 *
 * <p>A {@link PlacementComparatorFactory} must be set via
 * {@link #withPlacementComparatorBuilderFactory(PlacementComparatorFactory)}. Each
 * {@link #build()} call derives the set of <em>disabled</em> attributes from the active
 * constraint flags and calls {@link PlacementComparatorFactory#build(java.util.Collection)},
 * producing a comparator that ignores constraint dimensions that are not active for this
 * run.
 *
 * <p>To use a fixed {@link PlacementComparator}, wrap it via
 * {@link PlacementComparatorFactory#of(PlacementComparator)}.
 *
 * <p>Subclasses may override {@link #buildComparator(PlacementComparatorFactory, List)} to
 * inspect or extend the disabled-attribute list before the factory produces its comparator.
 */
public class LoadAwarePlacementControlsBuilder implements PlacementControlsBuilder {

	protected BoxItemSource boxItems;
	protected PointControls pointControls;
	protected PointCalculator pointCalculator;
	protected Container container;
	protected Stack stack;
	protected Order order;

	/**
	 * Factory that produces a per-run comparator by calling
	 * {@link PlacementComparatorFactory#build(java.util.Collection)} with the constraint
	 * attributes that are inactive for this run. Must be set before calling {@link #build()}.
	 */
	protected PlacementComparatorFactory comparatorBuilderFactory;

	protected Comparator<BoxItem> boxItemComparator;

	protected boolean maxLoadWeight;
	protected boolean maxLoadPressure;
	protected boolean maxLoadBoxCount;
	protected boolean loadIdenticalBox;

	protected boolean fullSupport;
	protected boolean calculateSupport;

	/**
	 * Sets the {@link PlacementComparatorFactory} used to produce a per-run comparator.
	 *
	 * @param comparatorBuilderFactory factory to build from; must not be {@code null}
	 * @return {@code this}
	 */
	public LoadAwarePlacementControlsBuilder withPlacementComparatorBuilderFactory(
			PlacementComparatorFactory comparatorBuilderFactory) {
		this.comparatorBuilderFactory = comparatorBuilderFactory;
		return this;
	}

	public LoadAwarePlacementControlsBuilder withBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
		this.boxItemComparator = boxItemComparator;
		return this;
	}

	@Override
	public PlacementControlsBuilder withPointCalculator(PointCalculator pointCalculator) {
		this.pointCalculator = pointCalculator;
		return this;
	}

	@Override
	public PlacementControlsBuilder withBoxItems(BoxItemSource boxItems) {
		this.boxItems = boxItems;
		return this;
	}

	@Override
	public PlacementControlsBuilder withPointControls(PointControls pointControls) {
		this.pointControls = pointControls;
		return this;
	}

	@Override
	public PlacementControlsBuilder withStack(Stack stack) {
		this.stack = stack;
		return this;
	}

	@Override
	public PlacementControlsBuilder withContainer(Container container) {
		this.container = container;
		return this;
	}

	@Override
	public PlacementControlsBuilder withOrder(Order order) {
		this.order = order;
		return this;
	}

	@Override
	public PlacementControlsBuilder withMaxLoad(boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount) {
		this.maxLoadWeight   = maxLoadWeight;
		this.maxLoadPressure = maxLoadPressure;
		this.maxLoadBoxCount = maxLoadBoxCount;
		return this;
	}

	@Override
	public PlacementControlsBuilder withLoadIdenticalBox(boolean loadIdenticalBox) {
		this.loadIdenticalBox = loadIdenticalBox;
		return this;
	}

	@Override
	public PlacementControlsBuilder withStability(boolean calculateSupport, boolean fullSupport) {
		this.fullSupport      = fullSupport;
		this.calculateSupport = calculateSupport;
		return this;
	}

	/**
	 * Produces the final {@link PlacementComparator} by calling
	 * {@link PlacementComparatorFactory#build(java.util.Collection)} with the given disabled
	 * attributes.
	 *
	 * <p>Subclasses may override to add further attributes to {@code disabled} (e.g. to
	 * suppress additional dimensions) or to cast the factory to
	 * {@link com.github.skjolber.packing.comparator.placement.DefaultPlacementComparatorFactory}
	 * and append position dimensions before building.
	 *
	 * @param factory  the configured factory
	 * @param disabled mutable list of attributes to skip; subclasses may append to it
	 * @return the final comparator; must not be {@code null}
	 */
	protected PlacementComparator buildComparator(PlacementComparatorFactory factory,
			List<PlacementComparatorAttribute> disabled) {
		return factory.build(disabled);
	}

	/**
	 * Builds the disabled-attribute list from the active constraint flags.
	 * Attributes for inactive constraints are added so that the factory can skip them.
	 * Uses representative constants — ID-based matching in
	 * {@link com.github.skjolber.packing.comparator.placement.DefaultPlacementComparatorFactory}
	 * will suppress all entries whose attribute ID matches (e.g. both HIGHER and LOWER variants).
	 *
	 * @return a mutable list of disabled attributes; empty when all constraints are active
	 */
	private List<PlacementComparatorAttribute> buildDisabledList() {
		if (maxLoadWeight && maxLoadPressure && maxLoadBoxCount && loadIdenticalBox) {
			return new ArrayList<>(0);
		}
		List<PlacementComparatorAttribute> disabled = new ArrayList<>(4);
		if (!maxLoadWeight)    disabled.add(PlacementComparatorAttribute.HIGHER_MAX_LOAD_WEIGHT);
		if (!maxLoadPressure)  disabled.add(PlacementComparatorAttribute.HIGHER_MAX_LOAD_PRESSURE);
		if (!maxLoadBoxCount)  disabled.add(PlacementComparatorAttribute.HIGHER_MAX_LOAD_BOX_COUNT);
		if (!loadIdenticalBox) disabled.add(PlacementComparatorAttribute.NO_IDENTICAL_CONSTRAINT);
		return disabled;
	}

	@Override
	public PlacementControls build() {
		PlacementComparator effectiveComparator = buildComparator(comparatorBuilderFactory, buildDisabledList());

		if (maxLoadWeight || maxLoadPressure || maxLoadBoxCount || loadIdenticalBox) {
			boolean maxLoadWeightOnly = maxLoadWeight && !maxLoadPressure && !maxLoadBoxCount;

			if (maxLoadWeightOnly) {
				return new WeightLoadAwarePlacementControls(boxItems, pointControls, pointCalculator,
						container, stack, order, effectiveComparator, boxItemComparator, fullSupport);
			}

			if (!loadIdenticalBox) {
				return new WeightPressureCountLoadAwarePlacementControls(boxItems, pointControls, pointCalculator,
						container, stack, order, effectiveComparator, boxItemComparator, fullSupport);
			}

			return new WeightPressureCountIdenticalLoadAwarePlacementControls(boxItems, pointControls, pointCalculator,
					container, stack, order, effectiveComparator, boxItemComparator, fullSupport);
		}

		if (fullSupport) {
			return new FullSupportPlacementControls(boxItems, pointControls, pointCalculator,
					container, stack, order, effectiveComparator, boxItemComparator);
		}

		if (calculateSupport) {
			return new SupportPlacementControls(boxItems, pointControls, pointCalculator,
					container, stack, order, effectiveComparator, boxItemComparator);
		}

		return new ComparatorPlacementControls(boxItems, pointControls, pointCalculator,
				container, stack, order, effectiveComparator, boxItemComparator);
	}
}
