package com.github.skjolber.packing.validator.load;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;

/**
 * Builds a {@link LoadValidator} by inspecting the {@link BoxStackValue} of each
 * placement and adding only the validators that are relevant for the constraints
 * actually present in the placement list.
 *
 * <p>The following validators are added automatically when the corresponding constraint
 * is detected on at least one placement's stack value:
 * <ul>
 *   <li>{@link WeightLoadValidator} — when any stack value has
 *       {@link BoxStackValue#isMaxLoadWeight()} {@code true}</li>
 *   <li>{@link MaxPressureLoadValidator} — when any stack value has
 *       {@link BoxStackValue#isMaxLoadPressure()} {@code true}</li>
 *   <li>{@link MaxBoxCountLoadValidator} — when any stack value has
 *       {@link BoxStackValue#isMaxLoadBoxCount()} {@code true}</li>
 *   <li>{@link IdenticalBoxOnlyLoadValidator} — when any stack value has
 *       {@link BoxStackValue#isLoadIdenticalBoxOnly()} {@code true}</li>
 * </ul>
 *
 * <p>If no constraints are detected, {@link #build()} returns {@code null}.
 * If exactly one validator is needed, it is returned directly without wrapping.
 * Otherwise, all relevant validators are composed into a {@link CompositeLoadValidator}.
 *
 * <p>Usage example:
 * <pre>{@code
 * LoadValidator validator = new DefaultLoadValidatorBuilder()
 *         .withPlacements(placements)
 *         .withContainer(container)
 *         .build();
 * }</pre>
 */
public class DefaultLoadValidatorBuilder {

	private List<Placement> placements;
	private Container container;

	/**
	 * Sets the list of placements to inspect for load constraints.
	 *
	 * @param placements the placements; must not be {@code null}
	 * @return this builder
	 */
	public DefaultLoadValidatorBuilder withPlacements(List<Placement> placements) {
		this.placements = placements;
		return this;
	}

	/**
	 * Sets the container associated with this packing result.
	 * Currently stored for context; may be used for container-level constraints in the future.
	 *
	 * @param container the container; must not be {@code null}
	 * @return this builder
	 */
	public DefaultLoadValidatorBuilder withContainer(Container container) {
		this.container = container;
		return this;
	}

	/**
	 * Inspects the placements, detects which load constraints are present, and returns
	 * a validator covering exactly those constraints.
	 *
	 * @return a {@link LoadValidator} composed of the relevant individual validators,
	 *         or {@code null} if no load constraints were detected
	 * @throws IllegalStateException if placements have not been provided
	 */
	public LoadValidator build() {
		if(placements == null) {
			throw new IllegalStateException("placements must be set before calling build()");
		}

		boolean needWeight = false;
		boolean needPressure = false;
		boolean needBoxCount = false;
		boolean needIdentical = false;

		for(Placement placement : placements) {
			BoxStackValue sv = placement.getStackValue();

			if(!needWeight && sv.isMaxLoadWeight()) needWeight    = true;
			if(!needPressure && sv.isMaxLoadPressure()) needPressure  = true;
			if(!needBoxCount && sv.isMaxLoadBoxCount()) needBoxCount  = true;
			if(!needIdentical && sv.isLoadIdenticalBoxOnly()) needIdentical = true;

			if(needWeight && needPressure && needBoxCount && needIdentical) {
				break; // all constraint types found — no need to scan further
			}
		}

		List<LoadValidator> validators = new ArrayList<>(4);

		if(needWeight) validators.add(new WeightLoadValidator());
		if(needPressure) validators.add(new MaxPressureLoadValidator());
		if(needBoxCount) validators.add(new MaxBoxCountLoadValidator());
		if(needIdentical) validators.add(new IdenticalBoxOnlyLoadValidator());

		if(validators.isEmpty()) {
			return null;
		}
		if(validators.size() == 1) {
			return validators.get(0);
		}
		return new CompositeLoadValidator(validators);
	}
}
