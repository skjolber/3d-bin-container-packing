package com.github.skjolber.packing.packer.strategy.bruteforce;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerStrategy;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.iterator.ContainerItemPermutationIterator;
import com.github.skjolber.packing.packer.strategy.allocation.ContainerAllocationPlanner;

/**
 * Explores every available sequence of container types up to the container
 * limit. A session and its container indexes are pushed together at each
 * accepted prefix and popped together on backtracking. Siblings fork their
 * unchanged parent instead of repacking earlier containers.
 * Packing within a container remains determined by the underlying packager.
 * Instances are one-shot because the controls retain the selected result.
 */
public class BruteForceContainerStrategy implements ContainerStrategy {

	public interface Controls {
		/**
		 * Create an independent snapshot for a parallel search branch. Mutable
		 * result state must not be shared between branches.
		 */
		Controls copy();

		/**
		 * Check whether to attempt packaging the selected container.
		 *
		 * @return false if not
		 */
		boolean attempt(List<Container> containers, PackagerSession state, List<Integer> availableContainerIndexes, int selectedContainerIndex);

		/**
		 * Add result. Return false to stop the search.
		 *
		 * @return false if not
		 */

		boolean result(ContainerResult result);

		/**
		 * Get the (best) result.
		 *
		 * @return result
		 */
		ContainerResult getResult();
	}

	private final Controls controls;

	public BruteForceContainerStrategy() {
		this(new FewestContainersControls());
	}

	public BruteForceContainerStrategy(Controls controls) {
		this.controls = Objects.requireNonNull(controls);
	}

	@Override
	public ContainerResult pack(PackagerInterruptSupplier interrupt, PackagerSession packagerSession) throws PackagerInterruptedException {
		if(interrupt.getAsBoolean()) {
			throw new PackagerInterruptedException();
		}
		int maxLength = packagerSession.getMaxContainerCount();
		if(maxLength == 0) {
			return null;
		}
		ContainerItemPermutationIterator iterator = new ContainerItemPermutationIterator(maxLength);
		Deque<PackagerSession> branches = new ArrayDeque<>(maxLength);
		branches.addLast(packagerSession);
		iterator.push(packagerSession.getContainers());
		List<Container> packed = new ArrayList<>(maxLength);
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

			PackagerSession parent = branches.getLast();
			int containerIndex = iterator.next();
			if(!controls.attempt(packed, parent, iterator.getContainerIndexes(), containerIndex)) {
				continue;
			}

			// TODO too expensive
			PackagerSession branch = parent.fork();

			IntermediatePackagerResult result = branch.attempt(containerIndex, null, packed.size() + 1 == maxLength);
			if(result == null || result.isEmpty()) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				continue;
			}

			packed.add(branch.accept(result));

			if(branch.countRemainingBoxes() == 0) {
				if(!controls.result(new ContainerResult(branch.getContainerInventory().getCost(), packed))) {
					break;
				}
			} else if(packed.size() < maxLength && ContainerAllocationPlanner.canAllocate(branch, interrupt)) {
				List<Integer> containerIndexes = branch.getContainers();
				if(!containerIndexes.isEmpty()) {
					branches.addLast(branch);
					iterator.push(containerIndexes);
					continue;
				}
			}
			packed.remove(packed.size() - 1);
		}
		return controls.getResult();
	}

}
