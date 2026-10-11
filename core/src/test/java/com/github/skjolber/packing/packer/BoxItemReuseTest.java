package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;

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
 * Packing does not change the box items and containers passed in, so they can be packed again:
 *
 * <pre>
 *   [c][c]   [c][ ]    three cubes in two containers, twice
 * </pre>
 */
public class BoxItemReuseTest {

	@Test
	public void plainPackagerKeepsTheInput() {
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			// <figure>
			// container 1 of 2: row
			//   z                         z                         y                         z
			//                             1 +-------+-------+       1 +-------+-------+       1 +-------+
			//   | /-------/-------|   y     |       |       |         |       |       |         |       |
			//   |/       /       /|         | cube  | cube  |         | cube  | cube  |         | cube  |
			// 1 |-------|-------| | /       |       |       |         |       |       |         |       |
			//   |       |       | |/      0 +-------+-------+       0 +-------+-------+       0 +-------+
			//   | cube  | cube  | | 1       0       1       2   x     0       1       2   x     0       1   y
			//   |       |       |/
			// 0 |-------|-------|-- x
			//   0       1       2
			//
			// container 2 of 2: row
			//   z                 z                 y                 z
			//                     1 +-------+       1 +-------+       1 +-------+
			//   | /-------|   y     |       |         |       |         |       |
			//   |/       /|         | cube  |         | cube  |         | cube  |
			// 1 |-------| | /       |       |         |       |         |       |
			//   |       | |/      0 +-------+       0 +-------+       0 +-------+
			//   | cube  | | 1       0       1   x     0       1   x     0       1   y
			//   |       |/
			// 0 |-------|-- x
			//   0       1
			// </figure>
			assertPacksTwice(packager);
		}
	}

	@Test
	public void largestAreaFitFirstPackagerKeepsTheInput() {
		try (LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build()) {
			// <figure>
			// container 1 of 2: row
			//   z                         z                         y                         z
			//                             1 +-------+-------+       1 +-------+-------+       1 +-------+
			//   | /-------/-------|   y     |       |       |         |       |       |         |       |
			//   |/       /       /|         | cube  | cube  |         | cube  | cube  |         | cube  |
			// 1 |-------|-------| | /       |       |       |         |       |       |         |       |
			//   |       |       | |/      0 +-------+-------+       0 +-------+-------+       0 +-------+
			//   | cube  | cube  | | 1       0       1       2   x     0       1       2   x     0       1   y
			//   |       |       |/
			// 0 |-------|-------|-- x
			//   0       1       2
			//
			// container 2 of 2: row
			//   z                 z                 y                 z
			//                     1 +-------+       1 +-------+       1 +-------+
			//   | /-------|   y     |       |         |       |         |       |
			//   |/       /|         | cube  |         | cube  |         | cube  |
			// 1 |-------| | /       |       |         |       |         |       |
			//   |       | |/      0 +-------+       0 +-------+       0 +-------+
			//   | cube  | | 1       0       1   x     0       1   x     0       1   y
			//   |       |/
			// 0 |-------|-- x
			//   0       1
			// </figure>
			assertPacksTwice(packager);
		}
	}

	@Test
	public void bruteForcePackagerKeepsTheInput() {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			// <figure>
			// container 1 of 2: row
			//   z                         z                         y                         z
			//                             1 +-------+-------+       1 +-------+-------+       1 +-------+
			//   | /-------/-------|   y     |       |       |         |       |       |         |       |
			//   |/       /       /|         | cube  | cube  |         | cube  | cube  |         | cube  |
			// 1 |-------|-------| | /       |       |       |         |       |       |         |       |
			//   |       |       | |/      0 +-------+-------+       0 +-------+-------+       0 +-------+
			//   | cube  | cube  | | 1       0       1       2   x     0       1       2   x     0       1   y
			//   |       |       |/
			// 0 |-------|-------|-- x
			//   0       1       2
			//
			// container 2 of 2: row
			//   z                 z                 y                 z
			//                     1 +-------+       1 +-------+       1 +-------+
			//   | /-------|   y     |       |         |       |         |       |
			//   |/       /|         | cube  |         | cube  |         | cube  |
			// 1 |-------| | /       |       |         |       |         |       |
			//   |       | |/      0 +-------+       0 +-------+       0 +-------+
			//   | cube  | | 1       0       1   x     0       1   x     0       1   y
			//   |       |/
			// 0 |-------|-- x
			//   0       1
			// </figure>
			assertPacksTwice(packager);
		}
	}

	@Test
	public void fastBruteForcePackagerKeepsTheInput() {
		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			// <figure>
			// container 1 of 2: row
			//   z                         z                         y                         z
			//                             1 +-------+-------+       1 +-------+-------+       1 +-------+
			//   | /-------/-------|   y     |       |       |         |       |       |         |       |
			//   |/       /       /|         | cube  | cube  |         | cube  | cube  |         | cube  |
			// 1 |-------|-------| | /       |       |       |         |       |       |         |       |
			//   |       |       | |/      0 +-------+-------+       0 +-------+-------+       0 +-------+
			//   | cube  | cube  | | 1       0       1       2   x     0       1       2   x     0       1   y
			//   |       |       |/
			// 0 |-------|-------|-- x
			//   0       1       2
			//
			// container 2 of 2: row
			//   z                 z                 y                 z
			//                     1 +-------+       1 +-------+       1 +-------+
			//   | /-------|   y     |       |         |       |         |       |
			//   |/       /|         | cube  |         | cube  |         | cube  |
			// 1 |-------| | /       |       |         |       |         |       |
			//   |       | |/      0 +-------+       0 +-------+       0 +-------+
			//   | cube  | | 1       0       1   x     0       1   x     0       1   y
			//   |       |/
			// 0 |-------|-- x
			//   0       1
			// </figure>
			assertPacksTwice(packager);
		}
	}

	private static void assertPacksTwice(AbstractPackager<?> packager) {
		List<BoxItem> boxItems = List.of(new BoxItem(Box.newBuilder().withId("cube").withSize(1, 1, 1).withWeight(1).build(), 3));
		List<ContainerItem> containers = List.of(new ContainerItem(Container.newBuilder().withId("row").withSize(2, 1, 1).withMaxLoadWeight(10).build(), 2));
		for(int i = 0; i < 2; i++) {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containers)
					.withBoxItems(boxItems)
					.withMaxContainerCount(2)
					.build();
			figure(result);

			assertThat(result.getContainers()).extracting(c -> c.getStack().size()).containsExactlyInAnyOrder(2, 1);
			assertThat(boxItems.get(0).getCount()).isEqualTo(3);
			assertThat(containers.get(0).getCount()).isEqualTo(2);
		}
	}
}
