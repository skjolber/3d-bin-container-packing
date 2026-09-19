package com.github.skjolber.packing.packer.strategy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

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
 */
public class BruteForceContainerPackingStrategy implements ContainerPackingStrategy {

	/**
	 * Compare an optimistic outcome of a branch with the current best packing.
	 * Return a positive value if the branch could strictly improve the best;
	 * return zero or a negative value only when it is safe to skip the branch.
	 */
	@FunctionalInterface
	public interface PotentialComparator {
		int compare(List<Container> best, List<Container> prefix, PackagerAdapter state,
				List<Integer> containerIndexes, int selectedContainerIndex, int remainingSlots);
	}

	private static final Comparator<List<Container>> FEWEST_CONTAINERS = new FewestContainersComparator();

	/** A branch with remaining boxes needs at least one more container. */
	public static final PotentialComparator FEWEST_CONTAINERS_BOUND = new FewestContainersPotentialComparator();

	private static final PotentialComparator NO_BOUND = new NoPruningPotentialComparator();

	private final Comparator<List<Container>> comparator;
	private final PotentialComparator potentialComparator;

	/** Prefer fewer containers; retain preference order for ties. */
	public BruteForceContainerPackingStrategy() {
		this(FEWEST_CONTAINERS, FEWEST_CONTAINERS_BOUND);
	}

	/**
	 * @param comparator returns a positive value when the first complete packing is better
	 */
	public BruteForceContainerPackingStrategy(Comparator<List<Container>> comparator) {
		this(comparator, NO_BOUND);
	}

	/**
	 * @param potentialComparator a safe optimistic comparison; nonpositive prunes a branch
	 */
	public BruteForceContainerPackingStrategy(Comparator<List<Container>> comparator, PotentialComparator potentialComparator) {
		this.comparator = comparator;
		this.potentialComparator = potentialComparator;
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
		List<Container> best = null;
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
			int selected = iterator.next();
			int remainingSlots = maxLength - packed.size();
			if(best != null && potentialComparator.compare(best, packed, parent,
					iterator.getContainerIndexes(), selected, remainingSlots) <= 0) {
				continue;
			}
			PackagerAdapter branch = parent.fork();

			IntermediatePackagerResult result = branch.attempt(selected, null, remainingSlots == 1);
			if(result == null || result.isEmpty()) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				continue;
			}
			packed.add(branch.accept(result));

			if(branch.countRemainingBoxes() == 0) {
				if(best == null || comparator.compare(best, packed) < 0) {
					best = List.copyOf(packed);
				}
			} else if(packed.size() < maxLength) {
				List<Integer> containerIndexes = branch.getContainers(maxLength - packed.size());
				if(!containerIndexes.isEmpty()) {
					branches.addLast(branch);
					iterator.push(containerIndexes);
					continue;
				}
			}
			packed.remove(packed.size() - 1);
		}
		return best == null ? List.of() : best;
	}
}
