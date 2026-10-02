package com.github.skjolber.packing.virtualbox;

import static org.assertj.core.api.Assertions.*;
import static com.github.skjolber.packing.virtualbox.VirtualBoxLayoutTest.*;
import static com.github.skjolber.packing.virtualbox.VirtualBoxPackagerTest.assertValid;
import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import com.github.skjolber.packing.api.*;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
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
			assertThat(recording.counts).containsExactly(2);
			assertValid(result, List.of(a));
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
			assertThat(recording.counts).containsExactly(2, 3);
			assertValid(result, List.of(a, b));
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
			assertThat(result.isSuccess()).isFalse();
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
			assertThat(recording.counts).containsExactly(2, 3);
			assertThat(result.size()).isEqualTo(2);
			assertValid(result, List.of(a, b));
		}
	}

	/*
	 *                  A x 4
	 *                 /     \
	 *              A x 2   A x 2
	 *                 \     /
	 *               same cache key
	 *
	 * Both children use one generated layout collection. Refining a child again
	 * preserves exactly four original boxes.
	 */
	@Test
	void identicalChildrenReuseOperationLocalLayouts() {
		BoxItem original = item(1, 1, 1, 4);
		List<Container> containers = List.of(container(4, 2, 1));
		CountingGrids grids = new CountingGrids();
		VirtualBoxPacking packing = new VirtualBoxPacking();
		packing.add(VirtualBox.of(grids.generate(original, containers, 8, () -> false)));
		VirtualBoxLayoutCache cache = new VirtualBoxLayoutCache(List.of(original), containers, grids, 100, 8);
		VirtualBoxPlan plan = new VirtualBoxPlan(List.of(original), packing, cache, 10);
		assertThat(plan.refine(() -> false)).isTrue();
		assertThat(grids.calls).isEqualTo(2); // initial four-box grid plus one two-box grid
		assertThat(plan.frontier.get(0).virtualBox.getLayouts().get(0)).isSameAs(plan.frontier.get(1).virtualBox.getLayouts().get(0));
		assertThat(cache.cached.size()).isEqualTo(2);
		assertThat(plan.refine(() -> false)).isTrue();
		assertThat(grids.calls).isEqualTo(2); // singleton children need no geometry search
		long count = 0;
		for(var node : plan.frontier) {
			count += node.count;
		}
		assertThat(count).isEqualTo(4);
		assertThat(original.getCount()).isEqualTo(4);
		// Finish the tree: [A A] + [A] + [A] becomes one original A entry, count four.
		assertThat(plan.refine(() -> false)).isTrue();
		VirtualBoxPacking plain = plan.packing();
		assertThat(plain.hasVirtualBoxes()).isFalse();
		assertThat(plain.getItems()).hasSize(1);
		assertThat(plain.getItems().get(0).getCount()).isEqualTo(4);
		assertThat(plan.refine(() -> false)).isFalse();
	}

	/*
	 * A cancelled refinement leaves the current partition intact.
	 */
	@Test
	void cancellationDoesNotCommitAPartialSplit() {
		BoxItem original = item(1, 1, 1, 4);
		List<Container> containers = List.of(container(4, 1, 1));
		CountingGrids grids = new CountingGrids();
		VirtualBoxPacking packing = new VirtualBoxPacking();
		packing.add(VirtualBox.of(grids.generate(original, containers, 8, () -> false)));
		VirtualBoxPlan plan = new VirtualBoxPlan(List.of(original), packing,
				new VirtualBoxLayoutCache(List.of(original), containers, grids, 100, 8), 10);
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
