package com.github.skjolber.packing.virtualbox;

import static org.assertj.core.api.Assertions.*;
import static com.github.skjolber.packing.virtualbox.VirtualBoxLayoutTest.*;
import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;
import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import com.github.skjolber.packing.api.*;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;
import com.github.skjolber.packing.virtualbox.VirtualBoxPackagerTest.RecordingPackager;

class VirtualBoxRefinementTest {
	/*
	 * Five cubes have no whole filled rectangular layout in a 3 x 2 floor.
	 *
	 *        A x 5  ----split---->  [ A A A ]  +  [ A A ]
	 *
	 * Two delegate items are allowed; five loose cubes are not.
	 */
	@Test
	void splittingCanBringInitiallyUngroupedInventoryUnderTheCountLimit() throws IOException {
		try(BruteForcePackager delegate = BruteForcePackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			BoxItem a = item(1, 1, 1, 5);
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a).withContainerItems(new ContainerItem(container(3, 2, 1), 1))
					.withMaxDelegateBoxes(2).withMaxRefinements(1).build();
			// <figure>
			//   z   /-------/-------|           y   z                                 y                                 z
			//      /   D   /   E   /|               1 +-------+-------+-------+       2 +-------+-------+               1 +-------+-------+
			//   | /-------/-------/-------|   /       |       |       |       |         |       |       |                 |       |       |
			//   |/       /       /       /|  /        |   A   |   B   |   C   |         |   D   |   E   |                 |   C   |   E   |
			// 1 |-------|-------|-------| | / 2       |       |       |       |         |       |       |                 |       |       |
			//   |       |       |       | |/        0 +-------+-------+-------+       1 +-------+-------+-------+       0 +-------+-------+
			//   |   A   |   B   |   C   | | 1         0       1       2       3   x     |       |       |       |         0       1       2   y
			//   |       |       |       |/                                              |   A   |   B   |   C   |
			// 0 |-------|-------|-------|-- x                                           |       |       |       |
			//   0       1       2       3                                             0 +-------+-------+-------+
			//                                                                           0       1       2       3   x
			// </figure>
			figure(result);
			assertThat(recording.counts).containsExactly(2);
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a));
		}
	}
	/*
	 * Coarse A (four cubes) does not fit with a fixed horizontal B in a 3 x 2 floor.
	 *
	 *       +-----------+-----+         +-----------+-----+
	 *       |           |     |         |   A A     |  A  |
	 *       |     A     |     |   -->   +-----------+     +
	 *       |           |     |         |     B     |  A  |
	 *       +-----------+-----+         +-----------+-----+
	 *
	 * Only A is split. Three delegate items suffice; five loose boxes are disallowed.
	 */
	@Test
	void oneSelectiveSplitSucceedsBelowTheUngroupedCountLimit() throws IOException {
		try(BruteForcePackager delegate = BruteForcePackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			BoxItem a = item(1, 1, 1, 4), b = item(2, 1, 1, 1);
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a, b).withContainerItems(new ContainerItem(container(3, 2, 1), 1))
					.withMaxRefinements(1).withMaxDelegateBoxes(3).build();
			// <figure>
			//   z   /-------/---------------|   y   z                                 y                                 z
			//      /   B   /       E       /|       1 +-------+-------+-------+       2 +-------+---------------+       1 +-------+-------+
			//   | /-------/-------/-------| | /       |       |       |       |         |       |               |         |       |       |
			//   |/       /       /       /| |/        |   A   |   C   |   D   |         |   B   |       E       |         |   D   |   E   |
			// 1 |-------|-------|-------| | | 2       |       |       |       |         |       |               |         |       |       |
			//   |       |       |       | |/        0 +-------+-------+-------+       1 +-------+-------+-------+       0 +-------+-------+
			//   |   A   |   C   |   D   | | 1         0       1       2       3   x     |       |       |       |         0       1       2   y
			//   |       |       |       |/                                              |   A   |   C   |   D   |
			// 0 |-------|-------|-------|-- x                                           |       |       |       |
			//   0       1       2       3                                             0 +-------+-------+-------+
			//                                                                           0       1       2       3   x
			// </figure>
			figure(result);
			assertThat(recording.counts).containsExactly(2, 3);
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a, b));
		}
	}

	/*
	 * The same split would require three delegate items.
	 * A limit of two excludes both refinement and the five-box fallback.
	 */
	@Test
	void delegateCountLimitAppliesToRefinementAndFallback() throws IOException {
		try(BruteForcePackager delegate = BruteForcePackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(item(1, 1, 1, 4), item(2, 1, 1, 1))
					.withContainerItems(new ContainerItem(container(3, 2, 1), 1)).withMaxDelegateBoxes(2).build();
			PackagerResultAssert.assertThat(result).isNotSuccess();
			assertThat(recording.counts).containsExactly(2);
		}
	}

	/*
	 * Coarse solution uses two containers. A later attempt fails:
	 *
	 *       valid incumbent ---> failed refinement ---> retain incumbent
	 */
	@Test
	void failedRefinementDoesNotDiscardAnEarlierCompletePacking() throws IOException {
		try(BruteForcePackager delegate = BruteForcePackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			recording.failAttempt = 2;
			BoxItem a = item(1, 1, 1, 4), b = item(2, 1, 1, 1);
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a, b).withContainerItems(new ContainerItem(container(3, 2, 1), 2))
					.withMaxContainerCount(2).withMaxRefinements(1).build();
			// <figure>
			// container 1 of 2
			//   z   /-------/-------|   y   z                         y                         z
			//      /   C   /   D   /|       1 +-------+-------+       2 +-------+-------+       1 +-------+-------+
			//   | /-------/-------| | /       |       |       |         |       |       |         |       |       |
			//   |/       /       /| |/        |   A   |   B   |         |   C   |   D   |         |   B   |   D   |
			// 1 |-------|-------| | | 2       |       |       |         |       |       |         |       |       |
			//   |       |       | |/        0 +-------+-------+       1 +-------+-------+       0 +-------+-------+
			//   |   A   |   B   | | 1         0       1       2   x     |       |       |         0       1       2   y
			//   |       |       |/                                      |   A   |   B   |
			// 0 |-------|-------|-- x                                   |       |       |
			//   0       1       2                                     0 +-------+-------+
			//                                                           0       1       2   x
			//
			// container 2 of 2
			//   z                         z                         y                         z
			//                             1 +---------------+       1 +---------------+       1 +-------+
			//   | /---------------|   y     |               |         |               |         |       |
			//   |/               /|         |       A       |         |       A       |         |   A   |
			// 1 |---------------| | /       |               |         |               |         |       |
			//   |               | |/      0 +---------------+       0 +---------------+       0 +-------+
			//   |       A       | | 1       0               2   x     0               2   x     0       1   y
			//   |               |/
			// 0 |---------------|-- x
			//   0               2
			// </figure>
			figure(result);
			assertThat(recording.counts).containsExactly(2, 3);
			assertThat(result.size()).isEqualTo(2);
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a, b));
		}
	}

	/*
	 *                  A x 4
	 *                 /     \
	 *              A x 2   A x 2
	 *                 \     /
	 *            one node, two copies
	 *
	 * Both children use one generated layout collection and become a single node
	 * with two copies. The next refinement splits both copies at once.
	 */
	@Test
	void identicalChildrenBecomeOneNodeAndSplitTogether() {
		BoxItem original = item(1, 1, 1, 4);
		List<ContainerItem> containers = List.of(new ContainerItem(container(4, 2, 1), 1));
		CountingGrids grids = new CountingGrids();
		VirtualBoxLayoutCache cache = new VirtualBoxLayoutCache(List.of(original), containers, 1, grids, 100, 8);
		VirtualBoxPlan plan = new VirtualBoxPlan(List.of(original), cache, 10);
		plan.add(0, () -> false);
		assertThat(plan.refine(() -> false)).isTrue();
		assertThat(grids.calls).isEqualTo(2); // initial four-box grid plus one two-box grid
		assertThat(plan.frontier).hasSize(1);
		assertThat(plan.frontier.get(0).copies).isEqualTo(2);
		assertThat(plan.delegateCount()).isEqualTo(2);
		assertThat(cache.cached.size()).isEqualTo(2);
		// Two equal blocks: one counted delegate item, not two permutable types
		VirtualBoxPacking merged = plan.packing();
		assertThat(merged.getItems()).hasSize(1);
		assertThat(merged.getItems().get(0).getCount()).isEqualTo(2);
		// Both [A A] copies split into singletons, which need no geometry search
		assertThat(plan.refine(() -> false)).isTrue();
		assertThat(grids.calls).isEqualTo(2);
		VirtualBoxPacking plain = plan.packing();
		assertThat(plain.hasVirtualBoxes()).isFalse();
		assertThat(plain.getItems()).hasSize(1);
		assertThat(plain.getItems().get(0).getCount()).isEqualTo(4);
		assertThat(original.getCount()).isEqualTo(4);
		assertThat(plan.refine(() -> false)).isFalse();
	}

	/*
	 * Eight cubes, containers holding four: two copies of a 2 x 2 block.
	 *
	 *       [A A]   [A A]          [A A]   [A A]
	 *       [A A]   [A A]   -->    [A A]   ----- + [A A]
	 *
	 * Splitting both copies would need four delegate items; the limit is three,
	 * so only one copy is split.
	 */
	@Test
	void delegateLimitSplitsOnlySomeCopies() {
		BoxItem original = item(1, 1, 1, 8);
		List<ContainerItem> containers = List.of(new ContainerItem(container(2, 2, 1), 2));
		VirtualBoxPlan plan = new VirtualBoxPlan(List.of(original), new VirtualBoxLayoutCache(List.of(original), containers, 2, new CountingGrids(), 100, 8), 3);
		plan.add(0, () -> false);
		assertThat(plan.frontier).hasSize(1);
		assertThat(plan.frontier.get(0).copies).isEqualTo(2);
		assertThat(plan.refine(() -> false)).isTrue();
		assertThat(plan.delegateCount()).isEqualTo(3);
		assertThat(plan.frontier).extracting(node -> node.count).containsExactly(4, 2);
		assertThat(plan.frontier).extracting(node -> node.copies).containsExactly(1, 2);
		assertThat(plan.refine(() -> false)).isFalse();
	}

	/*
	 * 10,000 cubes in containers holding two: 5,000 equal blocks are one node,
	 * not 5,000 nodes, so planning and refinement stay linear in the number of items.
	 */
	@Test
	void manyEqualBlocksAreOneNode() {
		BoxItem original = item(1, 1, 1, 10_000);
		List<ContainerItem> containers = List.of(new ContainerItem(container(2, 1, 1), 10_000));
		VirtualBoxPlan plan = new VirtualBoxPlan(List.of(original),
				new VirtualBoxLayoutCache(List.of(original), containers, 10_000, new CountingGrids(), 10_000, 8), Integer.MAX_VALUE);
		plan.add(0, () -> false);
		assertThat(plan.frontier).hasSize(1);
		assertThat(plan.frontier.get(0).copies).isEqualTo(5_000);
		assertThat(plan.delegateCount()).isEqualTo(5_000);
		assertThat(plan.refine(() -> false)).isTrue();
		assertThat(plan.frontier).hasSize(1);
		assertThat(plan.frontier.get(0).virtualBox).isNull();
		assertThat(plan.frontier.get(0).count).isEqualTo(10_000);
	}

	/*
	 * The parent fits only the long container A. Its halves rank better in the square
	 * container B, but must keep a layout which fits A:
	 *
	 *   A: [A A A A A A A A]  -->  [A A A A] + [A A A A]     (B: 2 x 2 holds a half, not the parent)
	 */
	@Test
	void refinedChildrenKeepALayoutFittingTheParentContainer() {
		BoxItem original = item(1, 1, 1, 8);
		Container a = container(8, 1, 1);
		Container b = container(2, 2, 1);
		List<ContainerItem> containers = List.of(new ContainerItem(a, 1), new ContainerItem(b, 1));
		VirtualBoxPlan plan = new VirtualBoxPlan(List.of(original), new VirtualBoxLayoutCache(List.of(original), containers, 2, new CountingGrids(), 100, 1), 10);
		plan.add(0, () -> false);
		assertThat(plan.refine(() -> false)).isTrue();
		assertThat(plan.frontier).hasSize(1);
		List<VirtualBoxLayout> layouts = plan.frontier.get(0).virtualBox.getLayouts();
		assertThat(layouts).anyMatch(layout -> layout.getBoundingBox().dx() <= 8 && layout.getBoundingBox().dy() <= 1 && layout.getBoundingBox().dz() <= 1);
		assertThat(layouts).anyMatch(layout -> layout.getBoundingBox().equals(VirtualBoxBounds.of(2, 2, 1)));
	}

	/*
	 * A cancelled refinement leaves the current partition intact.
	 */
	@Test
	void cancellationDoesNotCommitAPartialSplit() {
		BoxItem original = item(1, 1, 1, 4);
		List<ContainerItem> containers = List.of(new ContainerItem(container(4, 1, 1), 1));
		CountingGrids grids = new CountingGrids();
		VirtualBoxPlan plan = new VirtualBoxPlan(List.of(original), new VirtualBoxLayoutCache(List.of(original), containers, 1, grids, 100, 8), 10);
		plan.add(0, () -> false);
		assertThat(plan.refine(() -> true)).isFalse();
		assertThat(plan.frontier).hasSize(1);
		assertThat(grids.calls).isEqualTo(1);
	}

	protected static class CountingGrids extends GridVirtualBoxLayoutGenerator {
		protected int calls;
		@Override
		public List<VirtualBoxLayout> generate(BoxItem item, int count, List<Container> containers, int maxLayouts, BooleanSupplier interrupt) {
			calls++;
			return super.generate(item, count, containers, maxLayouts, interrupt);
		}
	}
}
