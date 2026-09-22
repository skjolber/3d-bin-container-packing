package com.github.skjolber.packing.packer.strategy.allocation;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;
import com.github.skjolber.packing.packer.strategy.ContainerResult;
import com.github.skjolber.packing.packer.strategy.ContainerStrategy;
import com.github.skjolber.packing.packer.strategy.allocation.ContainerAllocationPlanner.Allocation;
import com.github.skjolber.packing.packer.strategy.allocation.ContainerAllocationPlanner.Objective;

/**
 * Executes a container allocation plan one container at a time.
 * <p>
 * Before each packing attempt, this strategy asks {@link ContainerAllocationPlanner}
 * for an allocation of all remaining box items or box-item groups to the available
 * container inventory. The allocation is an inexpensive feasibility model: it
 * considers item-to-container compatibility, weight, volume, inventory and the
 * remaining container limit, but does not attempt a three-dimensional placement.
 * </p>
 * <p>
 * The first container in the selected allocation is packed by the underlying
 * {@link PackagerAdapter}. If that placement succeeds, the accepted container
 * changes the remaining items and inventory, so the strategy calculates a fresh
 * allocation before choosing the next container. If placement fails, the selected
 * container type is excluded for the current iteration and another allocation is
 * requested. This separates inexpensive container selection from the more costly
 * packing attempt while still allowing the strategy to recover from a geometrically
 * infeasible allocation.
 * </p>
 *
 * @see FewestContainersFitContainerStrategy
 * @see LowestCostFitContainerStrategy
 */
abstract class AbstractContainerAllocationStrategy implements ContainerStrategy {

	private final Objective objective;

	protected AbstractContainerAllocationStrategy(Objective objective) {
		this.objective = objective;
	}

	@Override
	public ContainerResult pack(PackagerInterruptSupplier interrupt, PackagerAdapter adapter)
			throws PackagerInterruptedException {
		int limit = adapter.getMaxContainerCount();
		List<Container> packed = new ArrayList<>();
		while(adapter.countRemainingBoxes() > 0 && packed.size() < limit) {
			// A failed geometric packing attempt only excludes a type for this
			// container-selection iteration. The next accepted container changes the
			// remaining items and starts a new selection from the full inventory.
			boolean[] excluded = new boolean[adapter.getContainerItemsCalculator().getContainerItemCount()];
			IntermediatePackagerResult result = null;
			while(result == null || result.isEmpty()) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				// The allocation is a cheap compatibility/capacity plan, not proof that
				// the first container can accommodate a physical 3D placement.
				Allocation allocation = ContainerAllocationPlanner.plan(adapter, objective, excluded, interrupt);
				if(allocation == null || allocation.getContainerCount() == 0) {
					return null;
				}
				int containerIndex = allocation.getContainerIndex(0);
				result = adapter.attempt(containerIndex, null, adapter.getMaxContainerCount() == 1);
				if(result == null || result.isEmpty()) {
					// Do not ask the planner to choose this known-infeasible type again
					// until an accepted container changes the problem state.
					excluded[containerIndex] = true;
				}
			}
			// Acceptance updates both the remaining items and the calculator's
			// inventory, so the next outer-loop iteration must re-plan.
			packed.add(adapter.accept(result));
		}
		if(adapter.countRemainingBoxes() != 0) {
			return null;
		}
		return new ContainerResult(adapter.getContainerItemsCalculator().getCost(), packed);
	}
}
