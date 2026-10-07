package com.github.skjolber.packing.packer.strategy.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;
import com.github.skjolber.packing.api.packager.RemainingBoxItemGroup;
import com.github.skjolber.packing.api.packager.strategy.ContainerResult;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.cost.FixedContainerCostCalculator;
import com.github.skjolber.packing.packer.AbstractPackagerSession;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.packer.strategy.allocation.ContainerAllocationPlanner.Allocation;
import com.github.skjolber.packing.packer.strategy.allocation.ContainerAllocationPlanner.Objective;
import com.github.skjolber.packing.packer.strategy.bruteforce.BruteForceContainerStrategy;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;

class ContainerAllocationStrategyTest {

	@Test
	void fewestContainersStrategyChoosesOneLargeContainer() {
		PlainPackager packager = PlainPackager.newBuilder()
				.withContainerStrategyFactory((inventory, boxes, groups) -> new FewestContainersFitContainerStrategy())
				.build();
		try {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(List.of(
							new ContainerItem(container("small", 1), 2),
							new ContainerItem(container("large", 2), 1)))
					.withMaxContainerCount(2)
					.withBoxItems(new BoxItem(box("cube", 1), 2))
					.build();

			PackagerResultAssert.assertThat(result).isSuccess();
			assertThat(result.getContainers()).singleElement()
					.extracting(Container::getId).isEqualTo("large");
		} finally {
			packager.close();
		}
	}

	@Test
	void lowestCostStrategyChoosesTwoCheapContainers() {
		PlainPackager packager = PlainPackager.newBuilder()
				.withContainerStrategyFactory((inventory, boxes, groups) -> new LowestCostFitContainerStrategy())
				.build();
		try {
			Container small = container("small", 1);
			Container large = container("large", 2);
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(List.of(
							costed(small, 2, 10),
							costed(large, 1, 100)))
					.withMaxContainerCount(2)
					.withBoxItems(new BoxItem(box("cube", 1), 2))
					.build();

			PackagerResultAssert.assertThat(result).isSuccess();
			assertThat(result.getContainers()).extracting(Container::getId)
					.containsExactly("small", "small");
			assertThat(result.getCost()).isEqualTo(20);
		} finally {
			packager.close();
		}
	}

	@Test
	void allocatesWholeGroupsOnlyToContainersWhichFitThem() throws Exception {
		Container small = container("small", 1);
		Container large = container("large", 2);
		ContainerItemsCalculator calculator = calculator(List.of(
				new ContainerItem(small, 1), new ContainerItem(large, 1)), 2);
		Box cube = box("cube", 1);
		List<RemainingBoxItemGroup> groups = AbstractPackagerSession.toRemainingBoxItemGroups(List.of(
				new BoxItemGroup("pair", List.of(new BoxItem(cube, 2))),
				new BoxItemGroup("single", List.of(new BoxItem(cube, 1)))));
		PlanningSession session = new PlanningSession(calculator, null, groups);

		Allocation allocation = ContainerAllocationPlanner.plan(session, Objective.FEWEST_CONTAINERS,
				null, () -> false);

		assertNotNull(allocation);
		assertThat(allocation.getContainerCount()).isEqualTo(2);
		assertThat(allocation.getContainerIndex(0)).isEqualTo(1);
		assertThat(allocation.getContainerIndex(1)).isEqualTo(0);
	}

	@Test
	void bruteForceDoesNotDescendIntoAnUnallocatableBranch() throws Exception {
		Box smallBox = box("small-box", 1);
		Box longBox = Box.newBuilder().withId("long-box").withSize(2, 1, 1).withWeight(1).build();
		ContainerItemsCalculator calculator = calculator(List.of(
				new ContainerItem(container("small", 1), 1),
				new ContainerItem(container("long", 2), 1)), 2);
		List<Integer> attempts = new ArrayList<>();
		int[] containerQueries = new int[1];
		BranchSession session = new BranchSession(calculator,
				AbstractPackagerSession.toRemainingBoxItems(List.of(new BoxItem(smallBox), new BoxItem(longBox))),
				attempts, containerQueries);

		ContainerResult result = new BruteForceContainerStrategy().pack(() -> false, session);

		assertNotNull(result);
		assertThat(result.getPackList()).extracting(Container::getId).containsExactly("small", "long");
		assertThat(attempts).containsExactly(0, 1, 1);
		// Root and the feasible small-container prefix. The long-first prefix is
		// rejected by allocation before asking it for another container list.
		assertThat(containerQueries[0]).isEqualTo(2);
	}

	private static ContainerItemsCalculator calculator(List<ContainerItem> items, int count) {
		List<ContainerItem> controlled = new ArrayList<>(items.size());
		for(ContainerItem item : items) {
			controlled.add(new ContainerItem(item));
		}
		return new ContainerItemsCalculator(controlled, count);
	}

	private static ContainerItem costed(Container container, int count, long cost) {
		return new ContainerItem(container, count,
				new FixedContainerCostCalculator(cost, container.getVolume(), null, 0));
	}

	private static Box box(String id, int dx) {
		return Box.newBuilder().withId(id).withSize(dx, 1, 1).withWeight(1).build();
	}

	private static Container container(String id, int dx) {
		return Container.newBuilder().withId(id).withSize(dx, 1, 1).withMaxLoadWeight(dx).build();
	}

	private static class PlanningSession implements PackagerSession {

		protected final ContainerItemsCalculator calculator;
		protected final List<RemainingBoxItem> boxes;
		protected final List<RemainingBoxItemGroup> groups;

		private PlanningSession(ContainerItemsCalculator calculator, List<RemainingBoxItem> boxes,
				List<RemainingBoxItemGroup> groups) {
			this.calculator = calculator;
			this.boxes = boxes;
			this.groups = groups;
		}

		@Override public ContainerItemsCalculator getContainerInventory() { return calculator; }
		@Override public List<RemainingBoxItem> getRemainingBoxItems() { return boxes; }
		@Override public List<RemainingBoxItemGroup> getRemainingBoxItemGroups() { return groups; }
		@Override public ContainerItem getContainerItem(int index) { return calculator.getContainerItem(index); }
		@Override public int countRemainingBoxes() {
			if(boxes != null) {
				return boxes.stream().mapToInt(RemainingBoxItem::getCount).sum();
			}
			return groups.stream().mapToInt(RemainingBoxItemGroup::getBoxCount).sum();
		}
		@Override public int countRemainingBoxItemGroups() { return groups == null ? -1 : groups.size(); }
		@Override public int getMaxContainerCount() {
			int itemLimit = groups == null ? countRemainingBoxes() : groups.size();
			return Math.min(calculator.getContainerCount(), itemLimit);
		}
		@Override public long getRemainingVolume() { throw new UnsupportedOperationException(); }
		@Override public long getRemainingWeight() { throw new UnsupportedOperationException(); }
		@Override public IntermediatePackagerResult attempt(int index, IntermediatePackagerResult best, boolean abort) { throw new UnsupportedOperationException(); }
		@Override public IntermediatePackagerResult peek(int index, IntermediatePackagerResult existing) { throw new UnsupportedOperationException(); }
		@Override public Container accept(IntermediatePackagerResult result) { throw new UnsupportedOperationException(); }
		@Override public List<Integer> getContainers() { throw new UnsupportedOperationException(); }
		@Override public PackagerSession fresh() { throw new UnsupportedOperationException(); }
		@Override public PackagerSession fork() { throw new UnsupportedOperationException(); }
	}

	private static final class BranchSession extends PlanningSession {

		private final List<Integer> attempts;
		private final int[] containerQueries;

		private BranchSession(ContainerItemsCalculator calculator, List<RemainingBoxItem> boxes,
				List<Integer> attempts, int[] containerQueries) {
			super(calculator, boxes, null);
			this.attempts = attempts;
			this.containerQueries = containerQueries;
		}

		@Override
		public IntermediatePackagerResult attempt(int index, IntermediatePackagerResult best, boolean abort) {
			attempts.add(index);
			ContainerItem item = calculator.getContainerItem(index);
			return new IntermediatePackagerResult() {
				@Override public ContainerItem getContainerItem() { return item; }
				@Override public Stack getStack() { return new Stack(); }
				@Override public boolean isEmpty() { return false; }
			};
		}

		@Override
		public Container accept(IntermediatePackagerResult result) {
			boxes.remove(0);
			return calculator.toContainer(result.getContainerItem(), result.getStack());
		}

		@Override
		public List<Integer> getContainers() {
			containerQueries[0]++;
			List<Integer> indexes = new ArrayList<>();
			for(int i = 0; i < calculator.getContainerItemCount(); i++) {
				if(calculator.getContainerItem(i).isAvailable()) {
					indexes.add(i);
				}
			}
			return indexes;
		}

		@Override
		public PackagerSession fork() {
			List<RemainingBoxItem> copies = new ArrayList<>(boxes.size());
			for(RemainingBoxItem item : boxes) {
				copies.add(item.copy());
			}
			return new BranchSession(calculator.copy(), copies, attempts, containerQueries);
		}
	}
}
