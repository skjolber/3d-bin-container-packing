package com.github.skjolber.packing.virtualbox;

import static org.assertj.core.api.Assertions.*;
import static com.github.skjolber.packing.virtualbox.VirtualBoxLayoutTest.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.github.skjolber.packing.api.*;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.AbstractPackagerResultBuilder;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;

class VirtualBoxPackagerTest {
	/*
	 * One scheduled deadline spans the entire operation:
	 *
	 *       grid -------> grouped attempt ----> ungrouped attempt
	 *            |                  |                      |
	 *            +------------------+----------------------+
	 *                               |
	 *                    close shared supplier once
	 */
	@Test
	void sharesOneScheduledDeadlineAcrossBothAttempts() throws IOException {
		try(PlainPackager delegate = PlainPackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			AtomicInteger closes = new AtomicInteger();
			List<Runnable> scheduled = new ArrayList<>();
			PackagerInterruptSupplier interrupt = new PackagerInterruptSupplier() {
				public boolean getAsBoolean() { return false; }
				public void close() { closes.incrementAndGet(); }
			};
			recording.beforeAttempt = () -> {
				assertThat(closes.get()).isZero();
				assertThat(wrapper.scheduler.getQueue()).hasSize(1);
				scheduled.add(wrapper.scheduler.getQueue().peek());
			};
			BoxItem a = item(1, 1, 1, 2), b = item(1, 2, 1, 1);
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a, b)
					.withContainerItems(new ContainerItem(container(4, 2, 1), 1))
					.withCompareUngrouped(true).withInterruptDuration(60_000).withInterrupt(interrupt).build();
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a, b));
			assertThat(recording.counts).containsExactly(2, 3);
			assertThat(scheduled).hasSize(2);
			assertThat(scheduled.get(0)).isSameAs(scheduled.get(1));
			assertThat(wrapper.scheduler.getQueue()).isEmpty();
			assertThat(closes.get()).isEqualTo(1);
		}
	}

	/*
	 * Fire the existing scheduled deadline deterministically at the delegate
	 * boundary. Both the delegate and fallback decision observe that same expiry.
	 */
	@Test
	void scheduledExpiryStopsTheDelegateAndPreventsFallback() throws IOException {
		try(PlainPackager delegate = PlainPackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			recording.beforeAttempt = () -> wrapper.scheduler.getQueue().peek().run();
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(item(1, 1, 1, 4))
					.withContainerItems(new ContainerItem(container(4, 1, 1), 1)).withInterruptDuration(60_000).build();
			assertThat(result.isTimeout()).isTrue();
			PackagerResultAssert.assertThat(result).isNotSuccess();
			assertThat(recording.counts).containsExactly(1);
		}
	}
	/*
	 * Initial points reserve only the right half of the container:
	 *
	 *       +-----+-----+-----+-----+
	 *       | unavailable | A | A |
	 *       +-----+-----+-----+-----+
	 *
	 * The wrapper forwards this operation unchanged.
	 */
	@Test
	void initialPointsBypassAggregationWithoutLosingTheRestriction() throws IOException {
		try(PlainPackager delegate = PlainPackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			BoxItem a = item(1, 1, 1, 2);
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a)
					.withContainerItem(c -> c.withContainerItem(container(4, 1, 1), 1).withPoints(p -> p.withPoint(2, 0, 0, 2, 1, 1))).build();
			assertThat(recording.counts).containsExactly(2);
			PackagerResultAssert.assertThat(result).isSuccess();
			assertThat(result.get(0).getStack().getPlacements()).extracting(Placement::getAbsoluteX).containsExactlyInAnyOrder(2, 3);
		}
	}
	/*
	 * Container floor is 3 x 2. Four A cubes form only a 2 x 2 virtual box.
	 *
	 * Coarse attempt:                 Ungrouped fallback:
	 *
	 *      +-----+-----+-----+             +-----+-----+-----+
	 *      |     A     |     |             |  A  |  A  |  A  |
	 *      +           +-----+             +-----+-----+-----+
	 *      |     A     |     |             |     B     |  A  |
	 *      +-----+-----+-----+             +-----+-----+-----+
	 *
	 * Fixed B is 2 x 1: it does not fit beside the coarse A, but fits with loose cubes.
	 */
	@Test
	void realGeometricFailureFallsBackToOriginalBoxes() throws IOException {
		try(BruteForcePackager delegate = BruteForcePackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			BoxItem a = item(1, 1, 1, 4), b = item(2, 1, 1, 1);
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a, b).withContainerItems(new ContainerItem(container(3, 2, 1), 1))
					.withMaxRefinements(0).build();
			assertThat(recording.counts).containsExactly(2, 5);
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a, b));
		}
	}

	/*
	 * Same geometry as above, now with two available containers:
	 *
	 *       coarse:       [ virtual A ]  [ B ]       two containers
	 *       ungrouped:    [ A A A A B ]              one container
	 *
	 * Optional comparison retains the better ungrouped result.
	 */
	@Test
	void optionalComparisonCanImproveContainerCount() throws IOException {
		try(BruteForcePackager delegate = BruteForcePackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			BoxItem a = item(1, 1, 1, 4), b = item(2, 1, 1, 1);
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a, b).withContainerItems(new ContainerItem(container(3, 2, 1), 2))
					.withMaxContainerCount(2).withMaxRefinements(0).withCompareUngrouped(true).build();
			assertThat(recording.counts).containsExactly(2, 5);
			assertThat(result.size()).isEqualTo(1);
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a, b));
		}
	}

	/*
	 * Eight boxes exceed a grid limit of four. They are split into two equal rows,
	 * handed to the delegate as one item with count two:
	 *
	 *       +---+---+---+---+
	 *       | A | A | A | A |
	 *       +---+---+---+---+
	 *       | A | A | A | A |
	 *       +---+---+---+---+
	 */
	@Test
	void oversizedAssembliesAreSplitIntoEqualBlocks() throws IOException {
		try(PlainPackager delegate = PlainPackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			BoxItem a = item(1, 1, 1, 8);
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a).withContainerItems(new ContainerItem(container(4, 2, 1), 1))
					.withMaxGridBoxes(4).build();
			assertThat(recording.counts).containsExactly(2);
			assertThat(recording.types).containsExactly(1);
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a));
		}
	}
	/*
	 * 24 physical boxes --> one delegate item --> 24 original placements
	 *
	 *       +---+---+---+---+       Three identical layers.
	 *       | A | A | A | A |       Each layer is 4 x 2.
	 *       +---+---+---+---+
	 *       | A | A | A | A |
	 *       +---+---+---+---+
	 */
	@Test
	void compressesAndExpandsAcrossDifferentPackagers() throws IOException {
		List<Packager<?>> delegates = List.of(PlainPackager.newBuilder().build(), BruteForcePackager.newBuilder().build(),
				FastBruteForcePackager.newBuilder().build(), LargestAreaFitFirstPackager.newBuilder().build());
		for(Packager<?> delegate : delegates) {
			try(delegate; RecordingPackager recording = new RecordingPackager(delegate); VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
				BoxItem item = item(1, 1, 1, 24);
				item.setGlobalIndex(42);
				item.setLocalIndex(17);
				ContainerItem container = new ContainerItem(container(4, 2, 3), 1);
				PackagerResult result = wrapper.newResultBuilder().withBoxItems(item).withContainerItems(container)
						.build();
				assertThat(recording.counts).containsExactly(1);
				PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(item));
				assertThat(item.getCount()).isEqualTo(24);
				assertThat(item.getGlobalIndex()).isEqualTo(42);
				assertThat(item.getLocalIndex()).isEqualTo(17);
				assertThat(item.getBox().getBoxItem()).isSameAs(item);
				assertThat(container.getCount()).isEqualTo(1);
			}
		}
	}

	/*
	 * Distinct original A and B remain separate delegate items:
	 *
	 *       +-----------------+-----+
	 *       |        A        |  B  |
	 *       +-----------------+-----+
	 *
	 * The delegate receives both boxes; no mixed-item layout search is performed.
	 */
	@Test
	void distinctItemsAreNotCombined() throws IOException {
		try(PlainPackager delegate = PlainPackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			BoxItem a = item(3, 2, 1, 1), b = item(1, 2, 1, 1);
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a, b).withContainerItems(new ContainerItem(container(4, 2, 1), 1))
					.build();
			assertThat(recording.counts).containsExactly(2);
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a, b));
		}
	}

	/*
	 * A delegate rejecting the whole virtual box:
	 *
	 *       [ A A A A ] --failure--> [ A ] [ A ] [ A ] [ A ] --success-->
	 *
	 * Retry uses fresh inventory even if the first delegate mutated its counts.
	 */
	@Test
	void fallsBackWithFreshUngroupedInventory() throws IOException {
		try(PlainPackager delegate = PlainPackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			recording.failFirst = true;
			BoxItem a = item(1, 1, 1, 4);
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a).withContainerItems(new ContainerItem(container(4, 1, 1), 1))
					.withMaxRefinements(0).build();
			assertThat(recording.counts).containsExactly(1, 4);
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a));
			assertThat(a.getCount()).isEqualTo(4);
		}
	}

	/*
	 * Prime-sized item cannot become a whole grid in a 3 x 2 floor.
	 * It is split into a full row and a line:
	 *
	 *       +---+---+---+
	 *       | A | A |   |
	 *       +---+---+---+
	 *       | A | A | A |
	 *       +---+---+---+
	 */
	@Test
	void primeCountIsSplitIntoRowAndLine() throws IOException {
		try(PlainPackager delegate = PlainPackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			BoxItem a = item(1, 1, 1, 5);
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a).withContainerItems(new ContainerItem(container(3, 2, 1), 1))
					.build();
			assertThat(recording.counts).containsExactly(2);
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a));
		}
	}

	/*
	 * Two virtual lines in one container:
	 *
	 *       +-----+-----+-----+-----+
	 *       | A   | A   | B   | B   |
	 *       +-----+-----+-----+-----+
	 *
	 * At least one line is translated away from the origin during expansion.
	 */
	@Test
	void expandsAtTranslatedCoordinatesWithoutChangingOriginalOrientations() throws IOException {
		try(PlainPackager delegate = PlainPackager.newBuilder().build(); VirtualBoxPackager wrapper = new VirtualBoxPackager(delegate)) {
			BoxItem a = item(1, 1, 1, 2), b = item(1, 1, 1, 2);
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a, b).withContainerItems(new ContainerItem(container(4, 1, 1), 1))
					.build();
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a, b));
			assertThat(result.get(0).getStack().getPlacements()).extracting(Placement::getAbsoluteX).containsExactlyInAnyOrder(0, 1, 2, 3);
		}
	}

	/*
	 * Whole virtual A fits the first container; whole virtual B fits the second.
	 * Container count constraints and distinct original identities survive.
	 */
	@Test
	void conservesInventoryAcrossMultipleContainers() throws IOException {
		try(PlainPackager delegate = PlainPackager.newBuilder().build(); VirtualBoxPackager wrapper = new VirtualBoxPackager(delegate)) {
			BoxItem a = item(1, 1, 1, 2), b = item(1, 1, 1, 2);
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a, b).withContainerItems(new ContainerItem(container(2, 1, 1), 2))
					.withMaxContainerCount(2).build();
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a, b));
			assertThat(result.size()).isEqualTo(2);
		}
	}

	/*
	 * Chronological input remains unaggregated:
	 *
	 *       [ A ] [ A ] [ A ]   three delegate boxes, not one envelope
	 */
	@Test
	void orderedInputsBypassAggregation() throws IOException {
		try(PlainPackager delegate = PlainPackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			BoxItem ordered = item(1, 1, 1, 3);
			wrapper.newResultBuilder().withBoxItems(ordered).withOrder(Order.CHRONOLOGICAL)
					.withContainerItems(new ContainerItem(container(3, 1, 1), 1)).build();
			assertThat(recording.counts).containsExactly(3);
		}
	}

	/*
	 * One explicit box-item group remains a group; preprocessing cannot dissolve it.
	 */
	@Test
	void groupsPassThroughToDelegate() throws IOException {
		try(PlainPackager delegate = PlainPackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			BoxItemGroup group = new BoxItemGroup("group", List.of(item(1, 1, 1, 2)));
			PackagerResult result = wrapper.newResultBuilder().withBoxItemGroups(List.of(group))
					.withContainerItems(new ContainerItem(container(2, 1, 1), 1)).build();
			PackagerResultAssert.assertThat(result).isSuccess();
			assertThat(recording.groupCounts).containsExactly(1);
		}
	}

	/*
	 * Disabled preprocessing ---> unchanged physical count at delegate boundary.
	 */
	@Test
	void limitsCanDisablePreprocessing() throws IOException {
		try(PlainPackager delegate = PlainPackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			BoxItem a = item(1, 1, 1, 4);
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a).withContainerItems(new ContainerItem(container(4, 1, 1), 1))
					.withMaxGridBoxes(1).build();
			assertThat(recording.counts).containsExactly(4);
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a));
		}
	}

	/*
	 * An already expired operation never reaches preprocessing or the delegate.
	 * A caller-owned interrupt is closed once, after all attempts have finished.
	 */
	@Test
	void deadlineAndInterruptOwnershipSpanTheWholeOperation() throws IOException {
		try(PlainPackager delegate = PlainPackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			AtomicInteger closes = new AtomicInteger();
			PackagerInterruptSupplier interrupt = new PackagerInterruptSupplier() {
				public boolean getAsBoolean() { return false; }
				public void close() { closes.incrementAndGet(); }
			};
			PackagerResult expired = wrapper.newResultBuilder().withBoxItems(item(1, 1, 1, 2))
					.withContainerItems(new ContainerItem(container(2, 1, 1), 1)).withInterruptDeadline(0).withInterrupt(interrupt).build();
			assertThat(expired.isTimeout()).isTrue();
			assertThat(expired.isSuccess()).isFalse();
			assertThat(recording.counts).isEmpty();
			assertThat(closes.get()).isEqualTo(1);
		}
	}

	/*
	 * Optional quality comparison runs both representations; ties retain the
	 * successful aggregated packing without replacing original input inventory.
	 */
	@Test
	void optionalUngroupedComparisonRunsBothAttempts() throws IOException {
		try(PlainPackager delegate = PlainPackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			BoxItem a = item(1, 1, 1, 4);
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a).withContainerItems(new ContainerItem(container(4, 1, 1), 1))
					.withCompareUngrouped(true).build();
			assertThat(recording.counts).containsExactly(1, 4);
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a));
		}
	}

	/*
	 * 40 boxes (2 x 1 x 1) need three 6 x 3 x 2 containers. The whole count forms no
	 * fitting grid, so it is split into container-sized blocks:
	 *
	 *       +-----------+   +-----------+   +-------+
	 *       | 3 x 3 x 2 |   | 3 x 3 x 2 |   | A A A |
	 *       +-----------+   +-----------+   | A     |
	 *                                       +-------+
	 *
	 * Brute force receives four boxes of three types instead of 40 loose boxes:
	 * the two equal 18-box blocks as one item with count two, a row of three and one loose box.
	 */
	@Test
	void multiContainerCountsBecomeContainerSizedBlocks() throws IOException {
		try(BruteForcePackager delegate = BruteForcePackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			BoxItem a = item(2, 1, 1, 40);
			PackagerResult result = wrapper.newResultBuilder()
					.withBoxItems(a)
					.withContainerItems(new ContainerItem(container(6, 3, 2), 3))
					.withMaxContainerCount(3)
					.withInterruptDuration(60_000)
					.build();
			assertThat(recording.counts).containsExactly(4);
			assertThat(recording.types).containsExactly(3);
			assertThat(result.size()).isEqualTo(3);
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a));
		}
	}

	/*
	 * One large container (6 x 3 x 2, holds 18) and three small ones (4 x 3 x 2, hold 12).
	 * Blocks of 18 would need two large containers, so 36 boxes become three blocks of 12:
	 *
	 *       +-----------+   +-----------+   +-----------+
	 *       | 2 x 3 x 2 |   | 2 x 3 x 2 |   | 2 x 3 x 2 |
	 *       +-----------+   +-----------+   +-----------+
	 *
	 * The first delegate attempt succeeds. Refinement is disabled to show that no
	 * retry or ungrouped fallback is needed; blocks of 18 would only fit the single large container.
	 */
	@Test
	void blocksFitTheAvailableContainers() throws IOException {
		try(BruteForcePackager delegate = BruteForcePackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			BoxItem a = item(2, 1, 1, 36);
			PackagerResult result = wrapper.newResultBuilder()
					.withBoxItems(a)
					.withContainerItems(new ContainerItem(container(6, 3, 2), 1), new ContainerItem(container(4, 3, 2), 3))
					.withMaxContainerCount(4)
					.withMaxRefinements(0)
					.withInterruptDuration(60_000)
					.build();
			assertThat(recording.counts).containsExactly(3);
			assertThat(recording.types).containsExactly(1);
			assertThat(result.size()).isEqualTo(3);
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a));
		}
	}

	/*
	 * Two full containers are the volume lower bound; refinement cannot improve it.
	 *
	 *       +-------+   +-------+
	 *       | A A A |   | A A A |
	 *       +-------+   +-------+
	 */
	@Test
	void resultAtLowerBoundIsNotRefined() throws IOException {
		try(BruteForcePackager delegate = BruteForcePackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			BoxItem a = item(1, 1, 1, 6);
			PackagerResult result = wrapper.newResultBuilder()
					.withBoxItems(a)
					.withContainerItems(new ContainerItem(container(3, 1, 1), 2))
					.withMaxContainerCount(2)
					.withInterruptDuration(60_000)
					.build();
			assertThat(result.size()).isEqualTo(2);
			assertThat(recording.counts).containsExactly(2);
			PackagerResultAssert.assertThat(result).isSuccess().isStackedWithinConstraints().placesExactly(List.of(a));
		}
	}

	/*
	 * Wrapper lifetime is separate from delegate lifetime.
	 */
	@Test
	void closingWrapperDoesNotCloseDelegate() throws IOException {
		try(PlainPackager delegate = PlainPackager.newBuilder().build()) {
			VirtualBoxPackager wrapper = new VirtualBoxPackager(delegate);
			wrapper.close();
			assertThatThrownBy(wrapper::newResultBuilder).isInstanceOf(IllegalStateException.class);
			assertThat(delegate.newResultBuilder().withBoxItems(item(1, 1, 1, 1))
					.withContainerItems(new ContainerItem(container(1, 1, 1), 1)).build().isSuccess()).isTrue();
		}
	}

	protected static class RecordingPackager implements Packager<RecordingPackager.RecordingBuilder> {
		protected final Packager<?> delegate;
		protected final List<Integer> counts = new ArrayList<>();
		protected final List<Integer> types = new ArrayList<>();
		protected final List<Integer> groupCounts = new ArrayList<>();
		protected boolean failFirst;
		protected int failAttempt = -1;
		protected boolean stackFirst;
		protected Runnable beforeAttempt = () -> {};

		protected RecordingPackager(Packager<?> delegate) { this.delegate = delegate; }
		public RecordingBuilder newResultBuilder() { return new RecordingBuilder(); }
		public void close() {}

		class RecordingBuilder extends AbstractPackagerResultBuilder<RecordingBuilder> {
			public PackagerResult build() {
				int count = 0;
				for(BoxItem item : items) {
					count += item.getCount();
				}
				counts.add(count);
				types.add(items.size());
				groupCounts.add(itemGroups.size());
				beforeAttempt.run();
				if((failFirst && counts.size() == 1) || counts.size() == failAttempt) {
					items.forEach(item -> item.setCount(0));
					containers.forEach(item -> item.setCount(0));
					return new PackagerResult(List.of(), 0, false);
				}
				if(stackFirst && counts.size() == 1) {
					Container container = containers.get(0).getContainer().copy();
					int z = 0;
					for(BoxItem item : items) {
						BoxStackValue value = item.getBox().getStackValue(0);
						container.getStack().add(new Placement(value, -1, 0, 0, z, false));
						z += value.getDz();
					}
					return new PackagerResult(List.of(container), 0, false);
				}
				return delegate.newResultBuilder().withBoxItems(items).withBoxItemGroups(itemGroups).withContainerItems(new ArrayList<>(containers))
						.withMaxContainerCount(maxContainerCount).withOrder(order).withInterruptDeadline(deadline).withInterrupt(interrupt).build();
			}
		}
	}
}
