package com.github.skjolber.packing.validator.load;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.validator.SupportGraph;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;
import com.github.skjolber.packing.validator.load.reasons.NonIdenticalLoadBoxReason;

/**
 * Validates that only boxes of the same type are stacked on top of each placement that
 * has the identical-box-only constraint set by
 * {@link BoxStackValue#isLoadIdenticalBoxOnly()}.
 *
 * <p>Only placements for which {@link BoxStackValue#isLoadIdenticalBoxOnly()} returns
 * {@code true} are checked. Box identity is determined by {@link BoxItem} reference
 * equality: a supportee is considered identical when its {@link Placement#getBoxItem()}
 * returns the same instance as the constrained placement's box item.
 *
 * <p>The check is applied recursively through the entire supportee subtree so that
 * violations at any level above the constrained placement are detected.
 *
 * @see BoxStackValue#isLoadIdenticalBoxOnly()
 * @see SupportGraph
 */
public class IdenticalBoxOnlyLoadValidator implements LoadValidator {

	/**
	 * {@inheritDoc}
	 *
	 * <p>Iterates all placements that require identical boxes, then walks the entire
	 * supportee subtree of each such placement. Adds a {@link NonIdenticalLoadBoxReason}
	 * for every supportee whose {@link BoxItem} does not match.
	 *
	 * @return {@code true} if no identical-box constraints are violated; {@code false} otherwise
	 */
	@Override
	public boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons) {
		boolean valid = true;

		SupportGraph graph = new SupportGraph(list);
		for(Placement placement : list) {
			BoxStackValue stackValue = placement.getStackValue();

			if(!stackValue.isLoadIdenticalBoxOnly()) {
				continue;
			}

			BoxItem requiredBoxItem = placement.getBoxItem();

			if(!checkSupporteesIdentical(graph, placement, placement, requiredBoxItem, reasons, Collections.newSetFromMap(new IdentityHashMap<>()))) {
				valid = false;
			}
		}

		return valid;
	}

	/**
	 * Recursively checks that all supportees of {@code placement} carry the same
	 * {@link BoxItem} as {@code requiredBoxItem}. Adds a {@link NonIdenticalLoadBoxReason}
	 * for each violation found and returns {@code false} if any violation was detected.
	 *
	 * @param constrained the placement with the identical-box-only constraint, reported as the constrained placement
	 * @param placement the placement whose supportees are to be validated
	 * @param requiredBoxItem the box item that all supportees must match
	 * @param reasons the list to collect violation reasons into
	 * @param visited supportees already checked; each is checked (and reported) once, as one can be
	 *        reached through several paths (and walking every path grows exponentially)
	 * @return {@code true} if all supportees are identical; {@code false} otherwise
	 */
	private boolean checkSupporteesIdentical(SupportGraph graph, Placement constrained, Placement placement, BoxItem requiredBoxItem,
			List<ValidatorResultReason> reasons, Set<Placement> visited) {
		boolean valid = true;

		for(PlacementLoad load : graph.getSupportees(placement)) {
			Placement supportee = load.getPlacement();
			if(!visited.add(supportee)) {
				continue;
			}
			BoxItem supporteeBoxItem = supportee.getBoxItem();

			if(supporteeBoxItem != requiredBoxItem) {
				reasons.add(new NonIdenticalLoadBoxReason(constrained, supportee, requiredBoxItem, supporteeBoxItem));
				valid = false;
			}

			if(!checkSupporteesIdentical(graph, constrained, supportee, requiredBoxItem, reasons, visited)) {
				valid = false;
			}
		}

		return valid;
	}
}
