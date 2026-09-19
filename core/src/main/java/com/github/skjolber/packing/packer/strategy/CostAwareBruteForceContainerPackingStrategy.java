package com.github.skjolber.packing.packer.strategy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.cost.ContainerCostCalculator;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.ContainerItemPermutationIterator;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.ContainerItemsCostCalculator;
import com.github.skjolber.packing.packer.EstimatingContainerItemsCostCalculator;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * Searches available container sequences for the lowest total cost, breaking
 * ties in favor of fewer containers. Uses each calculator's minimum cost as
 * a lower bound to skip branches that cannot improve the best complete
 * packing. The actual cost is calculated from the packed load before its
 * result is accepted.
 */
public class CostAwareBruteForceContainerPackingStrategy implements ContainerPackingStrategy {

	private final ContainerItemsCostCalculator containerItemsCostCalculator;

	public CostAwareBruteForceContainerPackingStrategy() {
		this(new EstimatingContainerItemsCostCalculator());
	}

	public CostAwareBruteForceContainerPackingStrategy(ContainerItemsCostCalculator containerItemsCostCalculator) {
		this.containerItemsCostCalculator = Objects.requireNonNull(containerItemsCostCalculator);
	}

	@Override
	public List<Container> pack(int limit, PackagerInterruptSupplier interrupt, PackagerAdapter adapter) throws PackagerInterruptedException {
		if(interrupt.getAsBoolean()) {
			throw new PackagerInterruptedException();
		}
		if(limit < 0) {
			throw new IllegalArgumentException("Negative container limit");
		}
		int maxLength = adapter.getMaximumContainerCount(limit);
		if(maxLength == 0) {
			return List.of();
		}

		ContainerItemPermutationIterator iterator = new ContainerItemPermutationIterator(maxLength);
		Deque<PackagerAdapter> branches = new ArrayDeque<>(maxLength);
		branches.addLast(adapter);
		iterator.push(adapter.getContainers(maxLength));
		List<Container> packed = new ArrayList<>(maxLength);
		long[] prefixCosts = new long[maxLength + 1];
		List<Container> best = null;
		long bestCost = Long.MAX_VALUE;

		while(iterator.hasLevel()) {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}
			if(!iterator.hasNext()) {
				iterator.pop();
				branches.removeLast();
				if(!packed.isEmpty()) {
					packed.remove(packed.size() - 1);
				}
				continue;
			}

			int depth = packed.size();
			PackagerAdapter parent = branches.getLast();
			int selected = iterator.next();
			ContainerCostCalculator calculator = getCostCalculator(parent.getContainerItem(selected), selected);
			if(best != null && !canImprove(prefixCosts[depth], minimumCost(calculator, selected), depth + 1, bestCost, best.size())) {
				continue;
			}

			PackagerAdapter branch = parent.fork();
			IntermediatePackagerResult result = branch.attempt(selected, null, depth + 1 == maxLength);
			if(result == null || result.isEmpty()) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				continue;
			}

			ContainerCostCalculator resultCalculator = getCostCalculator(result.getContainerItem(), selected);
			long containerCost = resultCalculator.calculateCost(result.getStack().getWeight());
			if(containerCost < 0) {
				throw new IllegalStateException("Container cost must be non-negative for index " + selected);
			}
			long totalCost = Math.addExact(prefixCosts[depth], containerCost);
			if(best != null && !canImprove(totalCost, 0, depth + 1, bestCost, best.size())) {
				continue;
			}

			packed.add(branch.accept(result));
			if(branch.countRemainingBoxes() == 0) {
				if(best == null || totalCost < bestCost || (totalCost == bestCost && packed.size() < best.size())) {
					best = List.copyOf(packed);
					bestCost = totalCost;
				}
			} else if(packed.size() < maxLength) {
				List<Integer> containerIndexes = branch.getContainers(maxLength - packed.size());
				if(!containerIndexes.isEmpty() && (best == null || canImprove(totalCost,
						Math.max(minimumCost(branch, containerIndexes), branch.estimateMinimumCost(containerItemsCostCalculator, maxLength - packed.size())),
						packed.size() + 1, bestCost, best.size()))) {
					prefixCosts[packed.size()] = totalCost;
					branches.addLast(branch);
					iterator.push(containerIndexes);
					continue;
				}
			}
			packed.remove(packed.size() - 1);
		}
		return best == null ? List.of() : best;
	}

	private static ContainerCostCalculator getCostCalculator(ContainerItem item, int index) {
		ContainerCostCalculator calculator = item.getCostCalculator();
		if(calculator == null) {
			throw new IllegalStateException("Missing cost calculator for container index " + index);
		}
		return calculator;
	}

	private static long minimumCost(ContainerCostCalculator calculator, int index) {
		long cost = calculator.getMinimumCost();
		if(cost < 0) {
			throw new IllegalStateException("Minimum container cost must be non-negative for index " + index);
		}
		return cost;
	}

	private static long minimumCost(PackagerAdapter branch, List<Integer> containerIndexes) {
		long minimum = Long.MAX_VALUE;
		for(int index : containerIndexes) {
			long cost = minimumCost(getCostCalculator(branch.getContainerItem(index), index), index);
			if(cost < minimum) {
				minimum = cost;
			}
		}
		return minimum;
	}

	/** Minimum additional containers is a safe tie-break bound for equal costs. */
	private static boolean canImprove(long currentCost, long minimumAdditionalCost, int minimumContainers, long bestCost, int bestCount) {
		long headroom = bestCost - currentCost;
		return minimumAdditionalCost < headroom || (minimumAdditionalCost == headroom && minimumContainers < bestCount);
	}
}
