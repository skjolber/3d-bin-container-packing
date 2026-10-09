package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * Container types in preference order, all of which hold the box; the first is used:
 *
 * <pre>
 *   c0 [b][ ]      c1 [b][ ][ ]      c2 ...
 * </pre>
 *
 * With three or more container types, the container packing strategy searches for it; the packagers then reuse a result in
 * other containers which also hold it.
 */
public class FirstFittingContainerTest {

	@Test
	public void plainPackagerUsesTheFirstContainerWhichHoldsTheBoxes() {
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			// <figure>
			//   z                 z                 y                 z
			//                     1 +-------+       1 +-------+       1 +-------+
			//   | /-------|   y     |       |         |       |         |       |
			//   |/       /|         |  box  |         |  box  |         |  box  |
			// 1 |-------| | /       |       |         |       |         |       |
			//   |       | |/      0 +-------+       0 +-------+       0 +-------+
			//   |  box  | | 1       0       1   x     0       1   x     0       1   y
			//   |       |/
			// 0 |-------|-- x
			//   0       1
			// </figure>
			assertUsesTheFirstContainer(packager);
		}
	}

	@Test
	public void largestAreaFitFirstPackagerUsesTheFirstContainerWhichHoldsTheBoxes() {
		try (LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build()) {
			// <figure>
			//   z                 z                 y                 z
			//                     1 +-------+       1 +-------+       1 +-------+
			//   | /-------|   y     |       |         |       |         |       |
			//   |/       /|         |  box  |         |  box  |         |  box  |
			// 1 |-------| | /       |       |         |       |         |       |
			//   |       | |/      0 +-------+       0 +-------+       0 +-------+
			//   |  box  | | 1       0       1   x     0       1   x     0       1   y
			//   |       |/
			// 0 |-------|-- x
			//   0       1
			// </figure>
			assertUsesTheFirstContainer(packager);
		}
	}

	@Test
	public void bruteForcePackagerUsesTheFirstContainerWhichHoldsTheBoxes() {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			// <figure>
			//   z                 z                 y                 z
			//                     1 +-------+       1 +-------+       1 +-------+
			//   | /-------|   y     |       |         |       |         |       |
			//   |/       /|         |  box  |         |  box  |         |  box  |
			// 1 |-------| | /       |       |         |       |         |       |
			//   |       | |/      0 +-------+       0 +-------+       0 +-------+
			//   |  box  | | 1       0       1   x     0       1   x     0       1   y
			//   |       |/
			// 0 |-------|-- x
			//   0       1
			// </figure>
			assertUsesTheFirstContainer(packager);
		}
	}

	@Test
	public void fastBruteForcePackagerUsesTheFirstContainerWhichHoldsTheBoxes() {
		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			// <figure>
			//   z                 z                 y                 z
			//                     1 +-------+       1 +-------+       1 +-------+
			//   | /-------|   y     |       |         |       |         |       |
			//   |/       /|         |  box  |         |  box  |         |  box  |
			// 1 |-------| | /       |       |         |       |         |       |
			//   |       | |/      0 +-------+       0 +-------+       0 +-------+
			//   |  box  | | 1       0       1   x     0       1   x     0       1   y
			//   |       |/
			// 0 |-------|-- x
			//   0       1
			// </figure>
			assertUsesTheFirstContainer(packager);
		}
	}

	private static void assertUsesTheFirstContainer(AbstractPackager<?> packager) {
		for(int count : new int[] {2, 4}) {
			// one container, or several (then the packagers first check whether a single container holds the boxes)
			for(int maxContainerCount : new int[] {1, 2}) {
				List<ContainerItem> containers = new ArrayList<>();
				for(int i = 0; i < count; i++) {
					containers.add(new ContainerItem(Container.newBuilder().withId("c" + i).withSize(2 + i, 1, 1).withMaxLoadWeight(100).build(), 1));
				}
				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(containers)
						.withBoxItems(new BoxItem(Box.newBuilder().withId("box").withSize(1, 1, 1).withWeight(1).build(), 1))
						.withMaxContainerCount(maxContainerCount)
						.build();
				figure(result);

				assertThat(result.getContainers()).as("%d container types, max %d containers", count, maxContainerCount).extracting(Container::getId).containsExactly("c0");
			}
		}
	}
}
