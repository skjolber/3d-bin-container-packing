package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.PackagerInput;

/**
 * A session can attempt the same container more than once, for example when the container strategy first checks
 * whether a single container holds all boxes. Each attempt searches from the first permutation and rotation, and from
 * an empty container.
 */
public class BruteForceRepeatedAttemptTest {

	//
	//  container 2 x 1 x 1; boxes a, b (1 x 1 x 1) and c (2 x 1 x 1). Not all boxes fit, so an attempt searches all
	//  permutations, and ends at the last one (c, b, a):
	//
	//   +---+---+
	//   | a | b |    first permutation (a, b, c): two boxes
	//   +---+---+
	//
	//   +-------+
	//   |   c   |    last permutation (c, b, a): one box
	//   +-------+
	//
	private static List<BoxItem> boxItems() {
		return List.of(
				new BoxItem(Box.newBuilder().withId("a").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 1),
				new BoxItem(Box.newBuilder().withId("b").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 1),
				new BoxItem(Box.newBuilder().withId("c").withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 1));
	}

	private static List<ContainerItem> containers() {
		return ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder().withId("container").withSize(2, 1, 1).withMaxLoadWeight(3).build(), 3)
				.build();
	}

	@Test
	public void bruteForceAttemptsAgainFromTheFirstPermutation() throws PackagerInterruptedException {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			assertAttemptsAgainFromTheFirstPermutation(packager);
		}
	}

	@Test
	public void fastBruteForceAttemptsAgainFromTheFirstPermutation() throws PackagerInterruptedException {
		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			assertAttemptsAgainFromTheFirstPermutation(packager);
		}
	}

	@Test
	public void parallelBruteForceAttemptsAgainFromTheFirstPermutation() throws PackagerInterruptedException {
		try (ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build()) {
			assertAttemptsAgainFromTheFirstPermutation(packager);
		}
	}

	private static void assertAttemptsAgainFromTheFirstPermutation(AbstractPackager<?> packager) throws PackagerInterruptedException {
		PackagerSession session = packager.createSession(new PackagerInput(boxItems(), null, containers(), 3, Order.NONE), () -> false);

		assertThat(session.attempt(0, null, false).getStack().size()).isEqualTo(2);
		assertThat(session.attempt(0, null, false).getStack().size()).isEqualTo(2);
	}

	/**
	 * With a box item order, fast brute force searches the rotations of the boxes; an attempt ends at the last
	 * rotations, where these boxes (in a 10 x 8 x 6 container) fit three of five instead of four.
	 */
	@Test
	public void fastBruteForceInOrderAttemptsAgainFromTheFirstRotations() throws PackagerInterruptedException {
		List<BoxItem> boxItems = List.of(
				new BoxItem(Box.newBuilder().withId("a").withSize(3, 3, 4).withRotate2D().withWeight(1).build(), 1),
				new BoxItem(Box.newBuilder().withId("b").withSize(4, 3, 2).withRotate3D().withWeight(1).build(), 1),
				new BoxItem(Box.newBuilder().withId("c").withSize(5, 6, 4).withRotate3D().withWeight(1).build(), 1),
				new BoxItem(Box.newBuilder().withId("d").withSize(6, 6, 2).withRotate2D().withWeight(1).build(), 1),
				new BoxItem(Box.newBuilder().withId("e").withSize(5, 4, 3).withRotate3D().withWeight(1).build(), 1));
		List<ContainerItem> containers = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder().withId("container").withSize(10, 8, 6).withMaxLoadWeight(20).build(), 6)
				.build();

		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			PackagerSession session = packager.createSession(new PackagerInput(boxItems, null, containers, 6, Order.CHRONOLOGICAL), () -> false);

			assertThat(session.attempt(0, null, false).getStack().size()).isEqualTo(4);
			assertThat(session.attempt(0, null, false).getStack().size()).isEqualTo(4);
		}
	}

	@Test
	public void bruteForceAttemptsAgainAfterAnInterruptedAttempt() throws PackagerInterruptedException {
		// interrupt the first attempt during the search, after placing the first box
		AtomicInteger checks = new AtomicInteger();
		AtomicInteger interruptAt = new AtomicInteger(3);
		PackagerInterruptSupplier interrupt = () -> checks.incrementAndGet() == interruptAt.get();

		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			PackagerSession session = packager.createSession(new PackagerInput(boxItems(), null, containers(), 3, Order.NONE), interrupt);

			assertThatThrownBy(() -> session.attempt(0, null, false)).isInstanceOf(PackagerInterruptedException.class);

			interruptAt.set(-1);
			assertThat(session.attempt(0, null, false).getStack().size()).isEqualTo(2);
		}
	}
}
