package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.AbstractPackager;

/**
 * The boxes placed before a box which exceeds the container's remaining load weight are kept.
 */
public class BruteForceLoadWeightTest {

	@Test
	public void bruteForcePackagerFillsTheLoadWeight() {
		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			// <figure>
			// container 1 of 2: c
			//   z                         z                         y                         z
			//                             1 +-------+-------+       1 +-------+-------+       1 +-------+
			//   | /-------/-------|   y     |       |       |         |       |       |         |       |
			//   |/       /       /|         |   a   |   a   |         |   a   |   a   |         |   a   |
			// 1 |-------|-------| | /       |       |       |         |       |       |         |       |
			//   |       |       | |/      0 +-------+-------+       0 +-------+-------+       0 +-------+
			//   |   a   |   a   | | 1       0       1       2   x     0       1       2   x     0       1   y
			//   |       |       |/
			// 0 |-------|-------|-- x
			//   0       1       2
			//
			// container 2 of 2: c
			//   z                         z                         y                         z
			//                             1 +-------+-------+       1 +-------+-------+       1 +-------+
			//   | /-------/-------|   y     |       |       |         |       |       |         |       |
			//   |/       /       /|         |   a   |   a   |         |   a   |   a   |         |   a   |
			// 1 |-------|-------| | /       |       |       |         |       |       |         |       |
			//   |       |       | |/      0 +-------+-------+       0 +-------+-------+       0 +-------+
			//   |   a   |   a   | | 1       0       1       2   x     0       1       2   x     0       1   y
			//   |       |       |/
			// 0 |-------|-------|-- x
			//   0       1       2
			// </figure>
			assertFillsTheLoadWeight(packager);
		} finally {
			packager.close();
		}
	}

	@Test
	public void fastBruteForcePackagerFillsTheLoadWeight() {
		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();
		try {
			// <figure>
			// container 1 of 2: c
			//   z                         z                         y                         z
			//                             1 +-------+-------+       1 +-------+-------+       1 +-------+
			//   | /-------/-------|   y     |       |       |         |       |       |         |       |
			//   |/       /       /|         |   a   |   a   |         |   a   |   a   |         |   a   |
			// 1 |-------|-------| | /       |       |       |         |       |       |         |       |
			//   |       |       | |/      0 +-------+-------+       0 +-------+-------+       0 +-------+
			//   |   a   |   a   | | 1       0       1       2   x     0       1       2   x     0       1   y
			//   |       |       |/
			// 0 |-------|-------|-- x
			//   0       1       2
			//
			// container 2 of 2: c
			//   z                         z                         y                         z
			//                             1 +-------+-------+       1 +-------+-------+       1 +-------+
			//   | /-------/-------|   y     |       |       |         |       |       |         |       |
			//   |/       /       /|         |   a   |   a   |         |   a   |   a   |         |   a   |
			// 1 |-------|-------| | /       |       |       |         |       |       |         |       |
			//   |       |       | |/      0 +-------+-------+       0 +-------+-------+       0 +-------+
			//   |   a   |   a   | | 1       0       1       2   x     0       1       2   x     0       1   y
			//   |       |       |/
			// 0 |-------|-------|-- x
			//   0       1       2
			// </figure>
			assertFillsTheLoadWeight(packager);
		} finally {
			packager.close();
		}
	}

	@Test
	public void parallelBruteForcePackagerFillsTheLoadWeight() {
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build();
		try {
			assertFillsTheLoadWeight(packager);
		} finally {
			packager.close();
		}
	}

	private static void assertFillsTheLoadWeight(AbstractPackager<?> packager) {
		assertFillsTheLoadWeight(packager, 3, 2, 4);
		assertFillsTheLoadWeight(packager, 1, 1, 2);
	}

	//
	//  containers 3 x 1 x 1 with max load weight 2; four boxes of weight 1
	//
	//  [a][a][ ]   [a][a][ ]
	//
	//  a third box fits in a container, but exceeds its load weight
	//
	//  containers 1 x 1 x 1 with max load weight 1; two boxes of weight 1
	//
	//  [a]   [a]
	//
	private static void assertFillsTheLoadWeight(AbstractPackager<?> packager, int dx, int maxLoadWeight, int boxes) {
		Container container = Container.newBuilder().withId("c").withSize(dx, 1, 1).withMaxLoadWeight(maxLoadWeight).build();
		BoxItem boxItem = new BoxItem(Box.newBuilder().withId("a").withSize(1, 1, 1).withWeight(1).build(), boxes);

		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(List.of(new ContainerItem(container, 2)))
				.withBoxItems(List.of(boxItem))
				.withMaxContainerCount(2)
				.build();
		// parallel brute force picks between equally good packings by thread timing, so its figure would change between runs
		if (!(packager instanceof ParallelBruteForcePackager)) {
			figure(result);
		}

		String name = packager.getClass().getSimpleName() + " " + dx + " x 1 x 1";
		assertThat(result.isSuccess()).as(name).isTrue();
		assertThat(result.getContainers()).as(name).extracting(c -> c.getStack().size()).containsExactly(maxLoadWeight, maxLoadWeight);
	}
}
