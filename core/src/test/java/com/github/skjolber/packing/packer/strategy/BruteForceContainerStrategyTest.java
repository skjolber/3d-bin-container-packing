package com.github.skjolber.packing.packer.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.cost.FixedContainerCostCalculator;
import com.github.skjolber.packing.packer.AbstractPackagerAdapter;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.ControlledContainerItem;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

class BruteForceContainerStrategyTest {

	@Test
	void forksWithoutMutatingTheSourceOrSiblingState() throws PackagerInterruptedException {
		Container container = Container.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(1).build();
		ContainerItemsCalculator calculator = new ContainerItemsCalculator(List.of(
				new ControlledContainerItem(container, 1), new ControlledContainerItem(container, 1)), 2);
		List<ContainerItemsCalculator> branchCalculators = new ArrayList<>();
		List<Integer> attemptedCounts = new ArrayList<>();
		int[] resetCalls = new int[1];
		TestAdapter source = new TestAdapter(calculator, branchCalculators, attemptedCounts, resetCalls);

		ContainerResult result = strategy(
				(first, second) -> Integer.compare(first.size(), second.size())).pack(() -> false, source);

		assertEquals(1, result.getPackList().size());
		assertEquals(2, branchCalculators.size());
		assertEquals(0, resetCalls[0]);
		assertEquals(List.of(1, 1), attemptedCounts);
		assertEquals(1, source.getContainerItem(0).getCount());
		assertEquals(1, source.getContainerItem(1).getCount());
	}

	@Test
	void backtracksUsingEligibilityFromEachPackingState() throws PackagerInterruptedException {
		Container container = Container.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(1).build();
		ContainerItemsCalculator calculator = new ContainerItemsCalculator(List.of(
				new ControlledContainerItem(container, 2), new ControlledContainerItem(container, 1)), 2);
		List<ContainerItemsCalculator> branches = new ArrayList<>();
		List<Integer> attempts = new ArrayList<>();
		int[] resets = new int[1];
		List<List<Integer>> completed = new ArrayList<>();
		int[] queries = new int[1];
		TestAdapter source = new TestAdapter(calculator, branches, attempts, resets, 2, completed, queries);

		ContainerResult result = strategy(
				(first, second) -> Integer.compare(first.size(), second.size())).pack(() -> false, source);

		assertEquals(2, result.getPackList().size());
		assertEquals(List.of(List.of(0, 0), List.of(1, 0)), completed);
		// The second choice at each depth is taken from the saved list;
		// eligibility is computed only once for each accepted prefix.
		assertEquals(3, queries[0]);
		assertEquals(List.of(2, 1, 1, 2), attempts);
		assertEquals(0, resets[0]);
		assertEquals(4, branches.size());
		assertEquals(2, source.getContainerItem(0).getCount());
		assertEquals(1, source.getContainerItem(1).getCount());
	}

	@Test
	void keepsParentAdaptersForNestedSiblings() throws PackagerInterruptedException {
		Container container = Container.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(1).build();
		ContainerItemsCalculator calculator = new ContainerItemsCalculator(List.of(
				new ControlledContainerItem(container, 1), new ControlledContainerItem(container, 1),
				new ControlledContainerItem(container, 1)), 3);
		List<Integer> attempts = new ArrayList<>();
		int[] resets = new int[1];
		List<List<Integer>> completed = new ArrayList<>();
		int[] queries = new int[1];
		TestAdapter source = new TestAdapter(calculator, new ArrayList<>(), attempts, resets, 3, completed, queries);

		strategy((first, second) -> Integer.compare(first.size(), second.size())).pack(() -> false, source);

		assertEquals(List.of(List.of(0, 1, 2), List.of(0, 2, 1),
				List.of(1, 0, 2), List.of(1, 2, 0)), completed);
		assertEquals(10, attempts.size());
		assertEquals(7, queries[0]);
		assertEquals(0, resets[0]);
	}

	@Test
	void triesEveryOrderedCombinationWhilePoppingBranches() throws PackagerInterruptedException {
		Container container = Container.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(1).build();
		ContainerItemsCalculator calculator = new ContainerItemsCalculator(List.of(
				new ControlledContainerItem(container, 1), new ControlledContainerItem(container, 1),
				new ControlledContainerItem(container, 1)), 3);
		List<Integer> attempts = new ArrayList<>();
		List<List<Integer>> completed = new ArrayList<>();
		int[] queries = new int[1];
		int[] resets = new int[1];
		TestAdapter source = new TestAdapter(calculator, new ArrayList<>(), attempts, resets,
				3, completed, queries) {
			@Override
			public List<Integer> getContainers() {
				List<Integer> result = super.getContainers();
				if(countRemainingBoxes() == 3) {
					result.add(2);
				}
				return result;
			}
		};

		strategy((first, second) -> Integer.compare(first.size(), second.size())).pack(() -> false, source);

		assertEquals(List.of(List.of(0, 1, 2), List.of(0, 2, 1),
				List.of(1, 0, 2), List.of(1, 2, 0),
				List.of(2, 0, 1), List.of(2, 1, 0)), completed);
		assertEquals(15, attempts.size());
		assertEquals(10, queries[0]);
		assertEquals(0, resets[0]);
	}

	@Test
	void defaultBoundSkipsBranchesThatCanOnlyTie() throws PackagerInterruptedException {
		Container container = Container.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(1).build();
		ContainerItemsCalculator calculator = new ContainerItemsCalculator(List.of(
				new ControlledContainerItem(container, 1), new ControlledContainerItem(container, 1)), 1);
		List<Integer> attempts = new ArrayList<>();
		int[] queries = new int[1];
		TestAdapter source = new TestAdapter(calculator, new ArrayList<>(), attempts, new int[1],
				1, new ArrayList<>(), queries);

		new BruteForceContainerStrategy().pack(() -> false, source);

		assertEquals(List.of(1), attempts);
		assertEquals(1, queries[0]);
	}

	@Test
	void customPotentialComparatorPrunesSelectedContainers() throws PackagerInterruptedException {
		Container container = Container.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(1).build();
		ContainerItemsCalculator calculator = new ContainerItemsCalculator(List.of(
				new ControlledContainerItem(container, 1), new ControlledContainerItem(container, 1),
				new ControlledContainerItem(container, 1)), 3);
		List<Integer> attempts = new ArrayList<>();
		List<List<Integer>> completed = new ArrayList<>();
		TestAdapter source = new TestAdapter(calculator, new ArrayList<>(), attempts, new int[1],
				3, completed, new int[1]);
		List<Integer> checkedContainerIndexes = new ArrayList<>();

		strategy((first, second) -> Integer.compare(first.size(), second.size()),
				(best, prefix, state, containerIndexes, selected, slots) -> {
					checkedContainerIndexes.add(selected);
					assertEquals(3 - prefix.size(), slots);
					assertEquals(3 - prefix.size(), state.countRemainingBoxes());
					assertTrue(containerIndexes.contains(selected));
					return !prefix.isEmpty() || selected != 1;
				}).pack(() -> false, source);

		assertEquals(List.of(List.of(0, 1, 2), List.of(0, 2, 1)), completed);
		assertEquals(5, attempts.size());
		assertTrue(checkedContainerIndexes.contains(1));
	}

	@Test
	void limitsIteratorByAvailableContainersAndRemainingBoxes() throws PackagerInterruptedException {
		Container container = Container.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(1).build();
		ContainerItemsCalculator twoContainers = new ContainerItemsCalculator(List.of(
				new ControlledContainerItem(container, 1), new ControlledContainerItem(container, 1)), 2);
		int[] requested = new int[1];
		TestAdapter threeBoxes = new TestAdapter(twoContainers, new ArrayList<>(), new ArrayList<>(),
				new int[1], 3, new ArrayList<>(), new int[1]) {
			@Override
			public List<Integer> getContainers() {
				requested[0] = getContainerItemsCalculator().getContainerCount();
				return super.getContainers();
			}
		};
		new BruteForceContainerStrategy().pack(() -> false, threeBoxes);
		assertEquals(2, requested[0]);
		assertEquals(2, threeBoxes.getContainerItemsCalculator().getContainerCount());

		ContainerItemsCalculator manyContainers = new ContainerItemsCalculator(List.of(
				new ControlledContainerItem(container, Integer.MAX_VALUE)), Integer.MAX_VALUE);
		TestAdapter oneBox = new TestAdapter(manyContainers, new ArrayList<>(), new ArrayList<>(),
				new int[1]);
		assertEquals(Integer.MAX_VALUE, oneBox.getContainerItemsCalculator().getContainerCount());
		assertEquals(1, new BruteForceContainerStrategy().pack(() -> false, oneBox).getPackList().size());
	}

	@Test
	void lowestPriceControlsPrunesAnExpensiveContainerBeforeAttempt() throws PackagerInterruptedException {
		Container cheap = Container.newBuilder().withId("cheap").withSize(1, 1, 1).withMaxLoadWeight(1).build();
		Container expensive = Container.newBuilder().withId("expensive").withSize(1, 1, 1).withMaxLoadWeight(1).build();
		ContainerItemsCalculator calculator = new ContainerItemsCalculator(List.of(
				costed(cheap, 1, 5), costed(expensive, 1, 100)), 1);
		List<Integer> attempts = new ArrayList<>();
		TestAdapter source = new TestAdapter(calculator, new ArrayList<>(), attempts, new int[1]);

		ContainerResult result = new BruteForceContainerStrategy(new LowestPriceControls()).pack(() -> false, source);

		assertEquals(List.of("cheap"), result.getPackList().stream().map(Container::getId).toList());
		assertEquals(1, attempts.size());
	}

	@Test
	void lowestPriceControlsUsesNextContainerMinimumBeforeDescending() throws PackagerInterruptedException {
		Container cheap = Container.newBuilder().withId("cheap").withSize(1, 1, 1).withMaxLoadWeight(1).build();
		Container expensive = Container.newBuilder().withId("expensive").withSize(1, 1, 1).withMaxLoadWeight(1).build();
		ContainerItemsCalculator calculator = new ContainerItemsCalculator(List.of(
				costed(cheap, 2, 5), costed(expensive, 1, 6)), 2);
		List<Integer> attempts = new ArrayList<>();
		int[] queries = new int[1];
		TestAdapter source = new TestAdapter(calculator, new ArrayList<>(), attempts, new int[1],
				2, new ArrayList<>(), queries);

		ContainerResult result = new BruteForceContainerStrategy(new LowestPriceControls()).pack(() -> false, source);

		assertEquals(List.of("cheap", "cheap"), result.getPackList().stream().map(Container::getId).toList());
		assertEquals(List.of(2, 1), attempts);
		assertEquals(2, queries[0]);
	}

	private static ControlledContainerItem costed(Container container, int count, long cost) {
		return new ControlledContainerItem(new ContainerItem(container, count,
				new FixedContainerCostCalculator(cost, container.getVolume(), null, 0)));
	}

	private static BruteForceContainerStrategy strategy(Comparator<List<Container>> comparator) {
		return new BruteForceContainerStrategy(new TestControls(comparator, (best, prefix, state,
				containerIndexes, selected, slots) -> true));
	}

	private static BruteForceContainerStrategy strategy(Comparator<List<Container>> comparator,
			AttemptFilter attemptFilter) {
		return new BruteForceContainerStrategy(new TestControls(comparator, attemptFilter));
	}

	@FunctionalInterface
	private interface AttemptFilter {
		boolean attempt(List<Container> best, List<Container> prefix, PackagerAdapter state,
				List<Integer> containerIndexes, int selectedContainerIndex, int remainingSlots);
	}

	private static class TestControls implements BruteForceContainerStrategy.Controls {

		private final Comparator<List<Container>> comparator;
		private final AttemptFilter attemptFilter;
		private ContainerResult best;

		private TestControls(Comparator<List<Container>> comparator, AttemptFilter attemptFilter) {
			this.comparator = comparator;
			this.attemptFilter = attemptFilter;
		}

		@Override
		public boolean attempt(List<Container> containers, PackagerAdapter state,
				List<Integer> availableContainerIndexes, int selectedContainerIndex) {
			return best == null || attemptFilter.attempt(best.getPackList(), containers, state,
					availableContainerIndexes, selectedContainerIndex, state.getMaxContainerCount());
		}

		@Override
		public boolean result(ContainerResult result) {
			if(best == null || comparator.compare(best.getPackList(), result.getPackList()) < 0) {
				best = result;
			}
			return true;
		}

		@Override
		public ContainerResult getResult() {
			return best;
		}
	}

	private static class TestAdapter extends AbstractPackagerAdapter {

		private final List<ContainerItemsCalculator> branchCalculators;
		private final List<Integer> attemptedCounts;
		private final int[] resetCalls;
		private final int initialRemaining;
		private final List<List<Integer>> completed;
		private final int[] queryCalls;
		private final List<Integer> containerIndexes = new ArrayList<>();
		private final List<Integer> accepted = new ArrayList<>();
		private int remaining;

		private TestAdapter(ContainerItemsCalculator calculator, List<ContainerItemsCalculator> branchCalculators,
				List<Integer> attemptedCounts, int[] resetCalls) {
			super(calculator.getContainerItems(), calculator.getContainerCount());
			this.branchCalculators = branchCalculators;
			this.attemptedCounts = attemptedCounts;
			this.resetCalls = resetCalls;
			this.initialRemaining = 1;
			this.completed = null;
			this.queryCalls = null;
			this.remaining = 1;
		}

		private TestAdapter(ContainerItemsCalculator calculator, List<ContainerItemsCalculator> branchCalculators,
				List<Integer> attemptedCounts, int[] resetCalls, int initialRemaining,
				List<List<Integer>> completed, int[] queryCalls) {
			super(calculator.getContainerItems(), calculator.getContainerCount());
			this.branchCalculators = branchCalculators;
			this.attemptedCounts = attemptedCounts;
			this.resetCalls = resetCalls;
			this.initialRemaining = initialRemaining;
			this.completed = completed;
			this.queryCalls = queryCalls;
			this.remaining = initialRemaining;
		}

		@Override
		protected TestAdapter fresh(List<ControlledContainerItem> containers, int containerCount) {
			ContainerItemsCalculator calculator = new ContainerItemsCalculator(containers, containerCount);
			branchCalculators.add(calculator);
			return new TestAdapter(calculator, branchCalculators, attemptedCounts, resetCalls,
					initialRemaining, completed, queryCalls);
		}

		@Override
		public PackagerAdapter fork() {
			ContainerItemsCalculator calculator = packagerContainerItems.clone();
			branchCalculators.add(calculator);
			TestAdapter fork = new TestAdapter(calculator, branchCalculators, attemptedCounts, resetCalls,
					initialRemaining, completed, queryCalls);
			fork.remaining = remaining;
			fork.accepted.addAll(accepted);
			return fork;
		}

		@Override
		protected void resetState() {
			remaining = initialRemaining;
			accepted.clear();
			resetCalls[0]++;
		}

		@Override
		protected IntermediatePackagerResult copy(ControlledContainerItem containerItem,
				IntermediatePackagerResult result, int index) {
			return result;
		}

		@Override
		public IntermediatePackagerResult attempt(int containerIndex, IntermediatePackagerResult best,
				boolean abortOnAnyBoxTooBig) {
			attemptedCounts.add(getContainerItem(containerIndex).getCount());
			return new IntermediatePackagerResult() {
				@Override public ControlledContainerItem getContainerItem() { return TestAdapter.this.getContainerItem(containerIndex); }
				@Override public Stack getStack() { return new Stack(); }
				@Override public boolean isEmpty() { return false; }
			};
		}

		@Override
		public Container accept(IntermediatePackagerResult result) {
			for(int i = 0; i < packagerContainerItems.getContainerItemCount(); i++) {
				if(getContainerItem(i) == result.getContainerItem()) {
					accepted.add(i);
					break;
				}
			}
			Container container = packagerContainerItems.toContainer(result.getContainerItem(), result.getStack());
			remaining--;
			if(remaining == 0 && completed != null) {
				completed.add(List.copyOf(accepted));
			}
			return container;
		}

		@Override
		public List<Integer> getContainers() {
			if(queryCalls != null) {
				queryCalls[0]++;
			}
			containerIndexes.clear();
			for(int i = 0; i < packagerContainerItems.getContainerItemCount(); i++) {
				if(getContainerItem(i).isAvailable()
						&& (initialRemaining != 2 || remaining == initialRemaining || i == 0)
						&& (initialRemaining != 3 || !accepted.isEmpty() || i != 2)) {
					containerIndexes.add(i);
				}
			}
			return containerIndexes;
		}

		@Override
		public ControlledContainerItem getContainerItem(int index) {
			return packagerContainerItems.getContainerItem(index);
		}

		@Override
		public int countRemainingBoxes() {
			return remaining;
		}

		@Override
		public int countRemainingBoxItemGroups() {
			return -1;
		}

		@Override
		public List<BoxItem> getRemainingBoxItems() {
			return List.of();
		}

		@Override
		public List<BoxItemGroup> getRemainingBoxItemGroups() {
			return List.of();
		}

		@Override
		public long getRemainingVolume() {
			return remaining;
		}

		@Override
		public long getRemainingWeight() {
			return remaining;
		}
	}
}
