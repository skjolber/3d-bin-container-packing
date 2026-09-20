package com.github.skjolber.packing.packer.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.cost.ContainerCostCalculator;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/** Plans and executes container packing using the lowest observed cost. */
public class LowestCostContainerPackingStrategy implements ContainerStrategy {

	private final Comparator<IntermediatePackagerResult> intermediatePackagerResultComparator;

	public LowestCostContainerPackingStrategy(Comparator<IntermediatePackagerResult> comparator) {
		this.intermediatePackagerResultComparator = comparator;
	}

	protected static class CostPacking {

		private final int containerIndex;
		private final IntermediatePackagerResult result;
		private final long cost;
		private final int availableCount;
		private final long volume;
		private final long weight;
		private final int count;

		public CostPacking(int containerIndex, IntermediatePackagerResult result, long cost, int availableCount) {
			this.containerIndex = containerIndex;
			this.result = result;
			this.cost = cost;
			this.availableCount = availableCount;

			Stack stack = result.getStack();
			this.volume = stack.getVolume();
			this.weight = stack.getWeight();
			this.count = stack.size();
		}
	}

	protected static class CostPackingPlanNode {

		private final CostPackingPlanNode parent;
		private final CostPacking packing;
		private final long cost;
		private final long volume;
		private final long weight;
		private final int count;
		private final int depth;
		private final int nextIndex;
		private final int currentTypeCount;

		public CostPackingPlanNode(CostPackingPlanNode parent, CostPacking packing, long cost, long volume,
				long weight, int count, int depth, int nextIndex, int currentTypeCount) {
			this.parent = parent;
			this.packing = packing;
			this.cost = cost;
			this.volume = volume;
			this.weight = weight;
			this.count = count;
			this.depth = depth;
			this.nextIndex = nextIndex;
			this.currentTypeCount = currentTypeCount;
		}
	}

	protected static class CostPackingPlanKey {

		private final long volume;
		private final long weight;
		private final int count;
		private final int depth;
		private final int nextIndex;
		private final int currentTypeCount;

		public CostPackingPlanKey(CostPackingPlanNode node) {
			this.volume = node.volume;
			this.weight = node.weight;
			this.count = node.count;
			this.depth = node.depth;
			this.nextIndex = node.nextIndex;
			this.currentTypeCount = node.currentTypeCount;
		}

		@Override
		public int hashCode() {
			int result = Long.hashCode(volume);
			result = 31 * result + Long.hashCode(weight);
			result = 31 * result + count;
			result = 31 * result + depth;
			result = 31 * result + nextIndex;
			return 31 * result + currentTypeCount;
		}

		@Override
		public boolean equals(Object object) {
			if(this == object) {
				return true;
			}
			if(!(object instanceof CostPackingPlanKey other)) {
				return false;
			}
			return volume == other.volume && weight == other.weight && count == other.count && depth == other.depth && nextIndex == other.nextIndex && currentTypeCount == other.currentTypeCount;
		}
	}

	/**
	 * Pack using the lowest-cost bounded plan derived from actual container
	 * packing results. The plan is recalculated after each accepted container so
	 * later choices reflect the boxes which were actually packed.
	 */
	@Override
	public ContainerResult pack(PackagerInterruptSupplier interrupt, PackagerAdapter adapter) throws PackagerInterruptedException {
		List<Container> containerPackResults = new ArrayList<>();

		int limit = adapter.getMaxContainerCount();

		while(containerPackResults.size() < limit) {
			int remainingBoxCount = adapter.countRemainingBoxes();
			if(remainingBoxCount == 0) {
				return new ContainerResult(adapter.getContainerItemsCalculator().getCost(), containerPackResults);
			}

			int remainingContainerCount = Math.min(limit - containerPackResults.size(), remainingBoxCount);
			List<Integer> containerItemIndexes = adapter.getContainers();
			if(containerItemIndexes.isEmpty()) {
				return null;
			}

			List<CostPacking> packings = new ArrayList<>(containerItemIndexes.size());
			CostPacking completed = null;
			try {
				for(int containerIndex : containerItemIndexes) {
					if(interrupt.getAsBoolean()) {
						throw new PackagerInterruptedException();
					}

					IntermediatePackagerResult result = adapter.attempt(containerIndex, null, remainingContainerCount == 1);
					if(result == null) {
						if(interrupt.getAsBoolean()) {
							throw new PackagerInterruptedException();
						}
						continue;
					}
					if(result.isEmpty()) {
						continue;
					}

					Stack stack = result.getStack();
					ContainerItem containerItem = adapter.getContainerItem(containerIndex);
					ContainerCostCalculator calculator = containerItem.getCostCalculator();
					if(calculator == null) {
						throw new IllegalStateException("Missing cost calculator for container index " + containerIndex);
					}
					long cost = calculator.calculateCost(stack.getWeight());
					if(cost < 0L) {
						throw new IllegalStateException("Container cost must be non-negative for index " + containerIndex);
					}

					CostPacking packing = new CostPacking(containerIndex, result, cost, Math.min(containerItem.getCount(), remainingContainerCount));
					packings.add(packing);
					if(packing.count == remainingBoxCount && (completed == null || cost < completed.cost)) {
						completed = packing;
					}
				}
			} catch(PackagerInterruptedException e) {
				if(completed != null) {
					containerPackResults.add(adapter.accept(completed.result));
					return new ContainerResult(adapter.getContainerItemsCalculator().getCost(), containerPackResults);
				}
				throw e;
			}

			if(packings.isEmpty()) {
				return null;
			}

			List<CostPacking> plan;
			try {
				plan = planLowestCost(packings, remainingContainerCount, remainingBoxCount, adapter.getRemainingVolume(), adapter.getRemainingWeight(), interrupt);
			} catch(PackagerInterruptedException e) {
				if(completed != null) {
					containerPackResults.add(adapter.accept(completed.result));
					return new ContainerResult(adapter.getContainerItemsCalculator().getCost(), containerPackResults);
				}
				throw e;
			}
			CostPacking selected = plan.isEmpty() ? selectBestPacking(packings, adapter) : plan.get(0);

			containerPackResults.add(adapter.accept(selected.result));
			if(adapter.countRemainingBoxes() == 0) {
				return new ContainerResult(adapter.getContainerItemsCalculator().getCost(), containerPackResults);
			}
		}

		return null;
	}

	/**
	 * Find the cheapest multiset of the observed packing results which covers
	 * the remaining count, volume and weight. A uniform-cost search guarantees
	 * that the first complete node has the lowest total cost.
	 */
	protected List<CostPacking> planLowestCost(List<CostPacking> packings, int limit, int targetCount, long targetVolume, long targetWeight, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		Comparator<CostPackingPlanNode> comparator = (first, second) -> {
			int compare = Long.compare(first.cost, second.cost);
			if(compare != 0) {
				return compare;
			}
			compare = Integer.compare(second.count, first.count);
			if(compare != 0) {
				return compare;
			}
			compare = Long.compare(second.volume, first.volume);
			if(compare != 0) {
				return compare;
			}
			compare = Long.compare(second.weight, first.weight);
			if(compare != 0) {
				return compare;
			}
			return Integer.compare(first.depth, second.depth);
		};

		PriorityQueue<CostPackingPlanNode> queue = new PriorityQueue<>(comparator);
		queue.add(new CostPackingPlanNode(null, null, 0L, 0L, 0L, 0, 0, 0, 0));
		Map<CostPackingPlanKey, Long> bestCosts = new HashMap<>();

		while(!queue.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}

			CostPackingPlanNode node = queue.poll();
			if(node.count >= targetCount && node.volume >= targetVolume && node.weight >= targetWeight) {
				List<CostPacking> result = new ArrayList<>(node.depth);
				while(node.packing != null) {
					result.add(node.packing);
					node = node.parent;
				}
				Collections.reverse(result);
				return result;
			}
			if(node.depth >= limit) {
				continue;
			}

			for(int i = node.nextIndex; i < packings.size(); i++) {
				CostPacking packing = packings.get(i);
				int currentTypeCount = i == node.nextIndex ? node.currentTypeCount + 1 : 1;
				if(currentTypeCount > packing.availableCount) {
					continue;
				}

				long cost = Math.addExact(node.cost, packing.cost);
				long volume = addSaturated(node.volume, packing.volume, targetVolume);
				long weight = addSaturated(node.weight, packing.weight, targetWeight);
				int count = (int)Math.min(targetCount, (long)node.count + packing.count);

				CostPackingPlanNode next = new CostPackingPlanNode(node, packing, cost, volume, weight, count, node.depth + 1, i, currentTypeCount);
				CostPackingPlanKey key = new CostPackingPlanKey(next);
				Long previousCost = bestCosts.get(key);
				if(previousCost != null && previousCost <= cost) {
					continue;
				}
				bestCosts.put(key, cost);
				queue.add(next);
			}
		}

		return Collections.emptyList();
	}

	protected long addSaturated(long value, long increment, long maximum) {
		if(value >= maximum || increment >= maximum - value) {
			return maximum;
		}
		return value + increment;
	}

	protected CostPacking selectBestPacking(List<CostPacking> packings, PackagerAdapter adapter) {
		CostPacking best = null;
		for(int i = packings.size() - 1; i >= 0; i--) {
			CostPacking candidate = packings.get(i);
			if(best == null || intermediatePackagerResultComparator.compare(best.result, candidate.result) <= 0) {
				best = candidate;
			}
		}
		return best;
	}

}
