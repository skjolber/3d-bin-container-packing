package com.github.skjolber.packing.packer.strategy;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToLongFunction;

import com.github.skjolber.packing.api.Container;

/**
 * Prefers the packing with the lowest total cost. The cost function is supplied
 * by the caller because a packed {@link Container} does not retain its
 * {@code ContainerItem}'s cost calculator.
 */
public final class LowestCostContainersComparator implements Comparator<List<Container>> {

	private final ToLongFunction<Container> containerCost;

	public LowestCostContainersComparator(ToLongFunction<Container> containerCost) {
		this.containerCost = Objects.requireNonNull(containerCost);
	}

	/** Returns a positive value when the first packing is cheaper. */
	@Override
	public int compare(List<Container> first, List<Container> second) {
		return Long.compare(totalCost(second), totalCost(first));
	}

	private long totalCost(List<Container> containers) {
		long total = 0;
		for(Container container : containers) {
			long cost = containerCost.applyAsLong(container);
			if(cost < 0) {
				throw new IllegalStateException("Container cost must be non-negative");
			}
			total = Math.addExact(total, cost);
		}
		return total;
	}
}
