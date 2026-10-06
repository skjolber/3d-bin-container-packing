package com.github.skjolber.packing.packer.bruteforce;

import static com.github.skjolber.packing.packer.PackagerGoldenMasterTest.LOAD_WEIGHT_PRESSURE_COUNT;
import static com.github.skjolber.packing.packer.PackagerGoldenMasterTest.pack;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.PackagerInput;

/**
 * Brute force skips searches which cannot load more volume than the best result so far, when results with less
 * load volume always compare worse. Packing results must be the same as without skipping.
 */
public class BruteForceLoadVolumeBoundTest {

	private static final int SEEDS = 100;

	private static final int MAX_BOXES = 6;

	@Test
	public void bruteForceAttemptIsEmptyWhenTheContainerCannotLoadMoreThanTheBestResult() throws PackagerInterruptedException {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			assertAttemptIsEmptyWhenTheContainerCannotLoadMoreThanTheBestResult(packager);
		}
	}

	@Test
	public void fastBruteForceAttemptIsEmptyWhenTheContainerCannotLoadMoreThanTheBestResult() throws PackagerInterruptedException {
		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			assertAttemptIsEmptyWhenTheContainerCannotLoadMoreThanTheBestResult(packager);
		}
	}

	private static void assertAttemptIsEmptyWhenTheContainerCannotLoadMoreThanTheBestResult(AbstractPackager<?> packager) throws PackagerInterruptedException {
		// The small container holds one of the boxes, the large container both:
		//
		//   small [a]     large [a][b]
		//
		Box box = Box.newBuilder().withId("box").withSize(1, 1, 1).withWeight(1).build();
		List<ContainerItem> containers = List.of(
				new ContainerItem(Container.newBuilder().withId("small").withSize(1, 1, 1).withMaxLoadWeight(2).build(), 1),
				new ContainerItem(Container.newBuilder().withId("large").withSize(2, 1, 1).withMaxLoadWeight(2).build(), 1));
		PackagerSession session = packager.createSession(new PackagerInput(List.of(new BoxItem(box, 2)), null, containers, 1, Order.NONE), () -> false);

		IntermediatePackagerResult large = session.attempt(1, null, false);
		assertThat(large.getStack().size()).isEqualTo(2);

		assertThat(session.attempt(0, null, false).getStack().size()).isEqualTo(1);
		assertThat(session.attempt(0, large, false).isEmpty()).isTrue();
	}

	@Test
	public void bruteForceResultsAreTheSameWithoutTheBound() {
		BruteForceIntermediatePackagerResultComparator comparator = new BruteForceIntermediatePackagerResultComparator();
		try (BruteForcePackager bounded = BruteForcePackager.newBuilder().build();
				BruteForcePackager unbounded = BruteForcePackager.newBuilder().withComparator(withoutLoadVolumeBound(comparator)).build()) {
			for(int seed = 0; seed < SEEDS; seed++) {
				for(boolean groups : new boolean[] {false, true}) {
					assertThat(pack(bounded, seed, MAX_BOXES, 1, groups)).as("seed %d, groups %s", seed, groups).isEqualTo(pack(unbounded, seed, MAX_BOXES, 1, groups));
				}
			}
		}
	}

	@Test
	public void fastBruteForceResultsAreTheSameWithoutTheBound() {
		BruteForceIntermediatePackagerResultComparator comparator = new BruteForceIntermediatePackagerResultComparator();
		try (FastBruteForcePackager bounded = FastBruteForcePackager.newBuilder().build();
				FastBruteForcePackager unbounded = FastBruteForcePackager.newBuilder().withComparator(withoutLoadVolumeBound(comparator)).build()) {
			for(int seed = 0; seed < SEEDS; seed++) {
				for(boolean groups : new boolean[] {false, true}) {
					assertThat(pack(bounded, seed, MAX_BOXES, 1, groups)).as("seed %d, groups %s", seed, groups).isEqualTo(pack(unbounded, seed, MAX_BOXES, 1, groups));
				}
			}
		}
	}

	@Test
	public void loadBruteForceResultsAreTheSameWithoutTheBound() {
		BruteForceIntermediatePackagerResultComparator comparator = new BruteForceIntermediatePackagerResultComparator();
		try (BruteForcePackager bounded = BruteForcePackager.newBuilder().build();
				BruteForcePackager unbounded = BruteForcePackager.newBuilder().withComparator(withoutLoadVolumeBound(comparator)).build()) {
			for(int seed = 0; seed < SEEDS; seed++) {
				assertThat(pack(bounded, seed, MAX_BOXES, 1, false, LOAD_WEIGHT_PRESSURE_COUNT)).as("seed %d", seed)
						.isEqualTo(pack(unbounded, seed, MAX_BOXES, 1, false, LOAD_WEIGHT_PRESSURE_COUNT));
			}
		}
	}

	/** The same comparison, without declaring that load volume is compared first. */
	private static Comparator<IntermediatePackagerResult> withoutLoadVolumeBound(Comparator<IntermediatePackagerResult> comparator) {
		return (a, b) -> comparator.compare(a, b);
	}
}
