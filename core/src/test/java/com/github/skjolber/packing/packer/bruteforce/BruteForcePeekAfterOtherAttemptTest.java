package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.ControlledContainerItem;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * A brute force result shares the session's placements, and a later attempt for another container overwrites them.
 * Peek must therefore calculate the stack from the result's own permutation state, not read the shared placements.
 *
 * Boxes a (4 x 1 x 1) and b (1 x 1 x 1). Both fit the wide container, only b fits the narrow one:
 *
 *   +---+---+---+---+
 *   | b |   .   .   |
 *   +---+---+---+---+    wide 4 x 2 x 1: a and b
 *   |       a       |
 *   +---+---+---+---+
 *
 *   +---+---+---+
 *   |   .   .   |
 *   +---+---+---+        narrow 3 x 2 x 1: only b
 *   | b |   .   |
 *   +---+---+---+
 *
 * After attempting the narrow container, the shared placements hold only b. Peeking the wide result against the
 * narrow container must then still see box a and return null.
 */
public class BruteForcePeekAfterOtherAttemptTest {

	@Test
	public void bruteForcePeeksTheResultsOwnStack() throws PackagerInterruptedException {
		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			assertPeeksTheResultsOwnStack(packager);
		} finally {
			packager.close();
		}
	}

	@Test
	public void fastBruteForcePeeksTheResultsOwnStack() throws PackagerInterruptedException {
		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();
		try {
			assertPeeksTheResultsOwnStack(packager);
		} finally {
			packager.close();
		}
	}

	@Test
	public void parallelBruteForcePeeksTheResultsOwnStack() throws PackagerInterruptedException {
		ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build();
		try {
			assertPeeksTheResultsOwnStack(packager);
		} finally {
			packager.close();
		}
	}

	private static void assertPeeksTheResultsOwnStack(AbstractBruteForcePackager packager) throws PackagerInterruptedException {
		List<BoxItem> boxItems = List.of(
				new BoxItem(Box.newBuilder().withId("a").withSize(4, 1, 1).withWeight(1).build(), 1),
				new BoxItem(Box.newBuilder().withId("b").withSize(1, 1, 1).withWeight(1).build(), 1));
		Container wide = Container.newBuilder().withId("wide").withSize(4, 2, 1).withMaxLoadWeight(10).build();
		Container narrow = Container.newBuilder().withId("narrow").withSize(3, 2, 1).withMaxLoadWeight(10).build();
		PackagerAdapter adapter = packager.createBoxItemAdapter(boxItems,
				new ContainerItemsCalculator(List.of(new ControlledContainerItem(wide, 1), new ControlledContainerItem(narrow, 1))), () -> false);

		IntermediatePackagerResult wideResult = adapter.attempt(0, null, false);
		assertThat(wideResult.getStack().size()).isEqualTo(2);

		// overwrites the shared placements: only b fits
		assertThat(adapter.attempt(1, null, false).getStack().size()).isEqualTo(1);

		assertThat(adapter.peek(1, wideResult)).isNull();
	}
}
