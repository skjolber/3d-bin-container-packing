package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.ControlledContainerItem;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * An adapter can attempt the same container more than once, for example when the packager first checks whether a
 * single container holds all boxes. Each attempt searches from the first permutation and rotation.
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
	@Test
	public void bruteForceAttemptsAgainFromTheFirstPermutation() throws PackagerInterruptedException {
		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			assertAttemptsAgainFromTheFirstPermutation(packager);
		} finally {
			packager.close();
		}
	}

	@Test
	public void fastBruteForceAttemptsAgainFromTheFirstPermutation() throws PackagerInterruptedException {
		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();
		try {
			assertAttemptsAgainFromTheFirstPermutation(packager);
		} finally {
			packager.close();
		}
	}

	@Test
	public void parallelBruteForceAttemptsAgainFromTheFirstPermutation() throws PackagerInterruptedException {
		ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build();
		try {
			assertAttemptsAgainFromTheFirstPermutation(packager);
		} finally {
			packager.close();
		}
	}

	private static void assertAttemptsAgainFromTheFirstPermutation(AbstractBruteForcePackager packager) throws PackagerInterruptedException {
		List<BoxItem> boxItems = List.of(
				new BoxItem(Box.newBuilder().withId("a").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 1),
				new BoxItem(Box.newBuilder().withId("b").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 1),
				new BoxItem(Box.newBuilder().withId("c").withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 1));
		Container container = Container.newBuilder().withId("container").withSize(2, 1, 1).withMaxLoadWeight(3).build();
		PackagerAdapter adapter = packager.createBoxItemAdapter(boxItems, new ContainerItemsCalculator(List.of(new ControlledContainerItem(container, 3))), () -> false);

		assertThat(adapter.attempt(0, null, false).getStack().size()).isEqualTo(2);
		assertThat(adapter.attempt(0, null, false).getStack().size()).isEqualTo(2);
	}
}
