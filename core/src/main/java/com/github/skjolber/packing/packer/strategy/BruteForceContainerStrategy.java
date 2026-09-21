package com.github.skjolber.packing.packer.strategy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.ContainerItemPermutationIterator;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * Explores every available sequence of container types up to the container
 * limit. An adapter and its container indexes are pushed together at each
 * accepted prefix and popped together on backtracking. Siblings fork their
 * unchanged parent instead of repacking earlier containers.
 * Packing within a container remains determined by the underlying packager.
 * Instances are one-shot because the controls retain the selected result.
 */
public class BruteForceContainerStrategy implements ContainerStrategy {

	public interface Controls {
		/**
		 * Check whether to attempt packaging the selected container.
		 *
		 * @return false if not
		 */
		boolean attempt(List<Container> containers, PackagerAdapter state, List<Integer> availableContainerIndexes, int selectedContainerIndex);

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
	public ContainerResult pack(PackagerInterruptSupplier interrupt, PackagerAdapter packagerAdapter) throws PackagerInterruptedException {
		if(interrupt.getAsBoolean()) {
			throw new PackagerInterruptedException();
		}
		int maxLength = packagerAdapter.getMaxContainerCount();
		if(maxLength == 0) {
			return null;
		}
		ContainerItemPermutationIterator iterator = new ContainerItemPermutationIterator(maxLength);
		Deque<PackagerAdapter> branches = new ArrayDeque<>(maxLength);
		branches.addLast(packagerAdapter);
		iterator.push(packagerAdapter.getContainers());
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

			PackagerAdapter parent = branches.getLast();
			int containerIndex = iterator.next();
			if(!controls.attempt(packed, parent, iterator.getContainerIndexes(), containerIndex)) {
				continue;
			}

			// TODO too expensive
			PackagerAdapter branch = parent.fork();

			IntermediatePackagerResult result = branch.attempt(containerIndex, null, packed.size() + 1 == maxLength);
			if(result == null || result.isEmpty()) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				continue;
			}

			packed.add(branch.accept(result));

			if(branch.countRemainingBoxes() == 0) {
				if(!controls.result(new ContainerResult(branch.getContainerItemsCalculator().getCost(), packed))) {
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
