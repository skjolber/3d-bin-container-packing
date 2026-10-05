package com.github.skjolber.packing.packer.composite;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;
import com.github.skjolber.packing.validator.DefaultValidator;

/**
 * The squares of the Bouwkamp code 15x11A fill a 15 x 11 container exactly. The plain packager does not find
 * the arrangement, but brute force does. A larger container fits the squares with either packager:
 *
 * <pre>
 *   exact (15 x 11):  6 6 5 5 4 4 3 1 1, no gaps       larger (16 x 12): with gaps
 * </pre>
 */
public class CompositePackagerTest {

	private static final long INTERRUPT_DURATION = 10_000;

	private final DefaultValidator validator = new DefaultValidator();

	@AfterEach
	void closeValidator() throws Exception {
		validator.close();
	}

	@Test
	public void plainPackagerNeedsTheLargerContainer() throws Exception {
		List<ContainerItem> containers = exactAndLargerContainers();
		List<BoxItem> boxItems = squares();
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).build();

			assertThat(result.getContainers()).extracting(Container::getId).containsExactly("larger");
		}
	}

	@Test
	public void costlyPackagerFindsTheExactContainer() throws Exception {
		List<ContainerItem> containers = exactAndLargerContainers();
		List<BoxItem> boxItems = squares();
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(FastBruteForcePackager.newBuilder().build())
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).withInterruptDuration(INTERRUPT_DURATION).build();

			assertThat(result.getContainers()).extracting(Container::getId).containsExactly("exact");
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containers)
					.withMaxContainerCount(1)
					.withBoxItems(boxItems));
		}
	}

	@Test
	public void costlyPackagerIsNotUsedWhereTheCheapPackagerPacksAllBoxes() throws Exception {
		//  [a][b]  plain packs both boxes, so brute force is never attempted
		List<ContainerItem> containers = List.of(new ContainerItem(container("row", 2, 1), 1));
		List<BoxItem> boxItems = List.of(new BoxItem(square(1), 2));
		CountingFastBruteForcePackager costly = new CountingFastBruteForcePackager();
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(costly)
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).build();

			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containers)
					.withMaxContainerCount(1)
					.withBoxItems(boxItems));
		}
		assertThat(costly.getAttempts()).isZero();
	}

	@Test
	public void returnsTheBaselineWhenTheImprovementIsInterrupted() throws Exception {
		List<ContainerItem> containers = exactAndLargerContainers();
		List<BoxItem> boxItems = squares();
		AtomicBoolean interrupted = new AtomicBoolean();
		CountingFastBruteForcePackager costly = new CountingFastBruteForcePackager(() -> {
			interrupted.set(true);
			throw new PackagerInterruptedException();
		});
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(costly)
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).withInterrupt(interrupted::get).build();

			// the plain packager's result
			assertThat(result.getContainers()).extracting(Container::getId).containsExactly("larger");
			assertThat(result.isTimeout()).isFalse();
		}
		assertThat(costly.getAttempts()).isEqualTo(1);
	}

	@Test
	public void stopsUsingAPackagerWhenItIsInterruptedByItsBudget() throws Exception {
		List<ContainerItem> containers = exactAndLargerContainers();
		List<BoxItem> boxItems = squares();
		CountingFastBruteForcePackager costly = new CountingFastBruteForcePackager(() -> {
			throw new PackagerInterruptedException();
		});
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(costly, 60_000)
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).build();

			assertThat(result.getContainers()).extracting(Container::getId).containsExactly("larger");
		}
		assertThat(costly.getAttempts()).isEqualTo(1);
	}

	@Test
	public void budgetLimitsTheCostlyPackager() throws Exception {
		// brute force (not the fast variant) takes far longer than the budget to find the exact arrangement
		List<ContainerItem> containers = exactAndLargerContainers();
		List<BoxItem> boxItems = squares();
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(BruteForcePackager.newBuilder().build(), 1)
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).withInterruptDuration(INTERRUPT_DURATION).build();

			assertThat(result.getContainers()).extracting(Container::getId).containsExactly("larger");
		}
	}

	@Test
	public void acceptedResultsKeepThePackagersInSync() throws Exception {
		// The squares and one more 1 x 1 box: brute force fills the exact container, plain packs the extra box
		//
		//   exact: 6 6 5 5 4 4 3 1 1      single: 1
		//
		List<ContainerItem> containers = List.of(new ContainerItem(container("exact", 15, 11), 1), new ContainerItem(container("single", 1, 1), 1));
		List<BoxItem> boxItems = new ArrayList<>(squares());
		boxItems.add(new BoxItem(Box.newBuilder().withId("extra").withSize(1, 1, 1).withWeight(1).build(), 1));
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(FastBruteForcePackager.newBuilder().build())
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).withMaxContainerCount(2).withInterruptDuration(INTERRUPT_DURATION).build();

			assertThat(result.getContainers()).extracting(Container::getId).containsExactlyInAnyOrder("exact", "single");
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containers)
					.withMaxContainerCount(2)
					.withBoxItems(boxItems));
		}
	}

	@Test
	public void packsBoxItemGroups() throws Exception {
		//  first: [a][a]   second: [b][b]
		List<ContainerItem> containers = List.of(new ContainerItem(container("row", 2, 1), 2));
		List<BoxItemGroup> groups = List.of(
				new BoxItemGroup("first", List.of(new BoxItem(Box.newBuilder().withId("a").withSize(1, 1, 1).withWeight(1).build(), 2))),
				new BoxItemGroup("second", List.of(new BoxItem(Box.newBuilder().withId("b").withSize(1, 1, 1).withWeight(1).build(), 2))));
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(FastBruteForcePackager.newBuilder().build())
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItemGroups(groups).withMaxContainerCount(2).build();

			assertThat(result.size()).isEqualTo(2);
			for(Container container : result.getContainers()) {
				assertThat(container.getStack().getPlacements()).extracting(p -> p.getStackValue().getBox().getId()).containsOnly(container.getStack().getPlacements().get(0).getStackValue().getBox().getId());
			}
		}
	}

	@Test
	public void packsBoxItemGroupsWhichFitOneContainerType() throws Exception {
		// The first group fits only the big container. Plain may pack the groups out of order, which brute force
		// does not; a packager which cannot continue from an accepted result is not used any more.
		//
		//   big: [long  ]      small: [c]
		//
		List<ContainerItem> containers = List.of(new ContainerItem(container("small", 1, 1), 1), new ContainerItem(container("big", 2, 1), 1));
		List<BoxItemGroup> groups = List.of(
				new BoxItemGroup("long", List.of(new BoxItem(Box.newBuilder().withId("long").withSize(2, 1, 1).withWeight(1).build(), 1))),
				new BoxItemGroup("cube", List.of(new BoxItem(Box.newBuilder().withId("cube").withSize(1, 1, 1).withWeight(1).build(), 1))));
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(FastBruteForcePackager.newBuilder().build())
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItemGroups(groups).withMaxContainerCount(2).withInterruptDuration(INTERRUPT_DURATION).build();

			assertThat(result.getContainers())
					.extracting(c -> c.getId() + ":" + c.getStack().getPlacements().get(0).getStackValue().getBox().getId())
					.containsExactlyInAnyOrder("big:long", "small:cube");
		}
	}

	@Test
	public void skipsPackagersWhichDoNotSupportTheInput() throws Exception {
		// brute force does not support box order
		List<ContainerItem> containers = List.of(new ContainerItem(container("row", 2, 1), 1));
		List<BoxItem> boxItems = List.of(new BoxItem(square(1), 2));
		CountingFastBruteForcePackager costly = new CountingFastBruteForcePackager();
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(costly)
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).withOrder(Order.CHRONOLOGICAL).build();

			assertThat(result.isSuccess()).isTrue();
		}
		assertThat(costly.getAttempts()).isZero();
	}

	@Test
	public void usesAtMostAsManyContainersAsTheBaseline() throws Exception {
		List<ContainerItem> containers = exactAndLargerContainers();
		List<BoxItem> boxItems = squares();
		try (CompositePackager packager = CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(FastBruteForcePackager.newBuilder().build())
				.build()) {
			PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems).withMaxContainerCount(2).withInterruptDuration(INTERRUPT_DURATION).build();

			assertThat(result.size()).isEqualTo(1);
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containers)
					.withMaxContainerCount(2)
					.withBoxItems(boxItems));
		}
	}

	private static List<ContainerItem> exactAndLargerContainers() {
		return List.of(new ContainerItem(container("exact", 15, 11), 1), new ContainerItem(container("larger", 16, 12), 1));
	}

	/** The squares of the Bouwkamp code 15x11A. */
	private static List<BoxItem> squares() {
		return List.of(
				new BoxItem(square(6), 2),
				new BoxItem(square(5), 2),
				new BoxItem(square(4), 2),
				new BoxItem(square(3), 1),
				new BoxItem(square(1), 2));
	}

	private static Box square(int size) {
		return Box.newBuilder().withId("square-" + size).withSize(size, size, 1).withRotate3D().withWeight(1).build();
	}

	private static Container container(String id, int dx, int dy) {
		return Container.newBuilder().withId(id).withSize(dx, dy, 1).withMaxLoadWeight(100).build();
	}
}
