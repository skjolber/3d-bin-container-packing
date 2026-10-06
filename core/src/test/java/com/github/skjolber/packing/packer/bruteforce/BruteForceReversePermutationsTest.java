package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.AbstractPackager;

/**
 * Skipping reverse permutations assumes that a permutation and its reverse pack equally well. When the insertion order
 * matters, they do not, and the reverse permutations are searched too. Boxes a (extracted first) and b:
 *
 * <pre>
 *   opening at the front (x):    [b][a]|      on top of each other:   [a]
 *                                                                     [b]
 * </pre>
 *
 * Only the permutation (b, a) works; skipping reverse permutations would only try (a, b).
 */
public class BruteForceReversePermutationsTest {

	private static List<BoxItem> boxItems() {
		return List.of(
				new BoxItem(Box.newBuilder().withId("a").withSize(1, 1, 1).withWeight(1).build(), 1).withExtractionOrder(1),
				new BoxItem(Box.newBuilder().withId("b").withSize(1, 1, 1).withWeight(1).build(), 1).withExtractionOrder(2));
	}

	private static void assertPacks(AbstractPackager<?> packager, int dx, int dz, ContainerAccess access) {
		Container container = Container.newBuilder().withId("c").withSize(dx, 1, dz).withMaxLoadWeight(10).withAccess(access).build();
		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(List.of(new ContainerItem(container, 1)))
				.withBoxItems(boxItems())
				.withMaxContainerCount(1)
				.withInterruptDuration(10_000)
				.build();
		assertThat(result.isSuccess()).as("%s %s", packager.getClass().getSimpleName(), access).isTrue();
	}

	@Test
	public void bruteForceSearchesReversePermutationsWithExtractionOrders() {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().withSkipReversePermutations(true).build()) {
			assertPacks(packager, 2, 1, ContainerAccess.FRONT);
			assertPacks(packager, 1, 2, ContainerAccess.ANY);
			assertPacks(packager, 1, 2, ContainerAccess.TOP);
		}
	}

	@Test
	public void parallelBruteForceSearchesReversePermutationsWithExtractionOrders() {
		try (ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).withSkipReversePermutations(true).build()) {
			assertPacks(packager, 2, 1, ContainerAccess.FRONT);
			assertPacks(packager, 1, 2, ContainerAccess.ANY);
			assertPacks(packager, 1, 2, ContainerAccess.TOP);
		}
	}

	//
	//  With full support, the wide box w must be placed first, under the narrow box n; in the order (n, w), w would
	//  rest partly on n. Only the reverse of the box order works:
	//
	//    [n]
	//    [ w  ]
	//
	@Test
	public void bruteForceSearchesReversePermutationsWithFullSupport() {
		List<BoxItem> boxItems = List.of(
				new BoxItem(Box.newBuilder().withId("n").withSize(1, 1, 1).withRotate2D().withWeight(1).build(), 1),
				new BoxItem(Box.newBuilder().withId("w").withSize(2, 1, 1).withRotate2D().withWeight(1).build(), 1));
		List<AbstractPackager<?>> packagers = List.of(
				BruteForcePackager.newBuilder().withSkipReversePermutations(true).withRequireFullSupport(true).build(),
				ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).withSkipReversePermutations(true).withRequireFullSupport(true).build());
		for (AbstractPackager<?> packager : packagers) {
			try (packager) {
				Container container = Container.newBuilder().withId("c").withSize(2, 1, 2).withMaxLoadWeight(10).build();
				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(List.of(new ContainerItem(container, 1)))
						.withBoxItems(boxItems)
						.withMaxContainerCount(1)
						.withInterruptDuration(10_000)
						.build();
				assertThat(result.isSuccess()).as(packager.getClass().getSimpleName()).isTrue();
			}
		}
	}
}
