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
 * Each box fits one container type:
 *
 * <pre>
 *   big:   [long  ]      small: [c]
 * </pre>
 *
 * Packing attempts filter the box items per container, which changes their local indexes. Whether the remaining
 * boxes can still be assigned to the remaining containers must not depend on them.
 */
public class ContainerTypePerBoxTest {

	@Test
	public void plainPackagerUsesBothContainerTypes() {
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			// <figure>
			// container 1 of 2: big
			//   z                         z                         y                         z
			//                             1 +---------------+       1 +---------------+       1 +-------+
			//   | /---------------|   y     |               |         |               |         |       |
			//   |/               /|         |     long      |         |     long      |         | long  |
			// 1 |---------------| | /       |               |         |               |         |       |
			//   |               | |/      0 +---------------+       0 +---------------+       0 +-------+
			//   |     long      | | 1       0               2   x     0               2   x     0       1   y
			//   |               |/
			// 0 |---------------|-- x
			//   0               2
			//
			// container 2 of 2: small
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
			assertUsesBothContainerTypes(packager);
		}
	}

	@Test
	public void largestAreaFitFirstPackagerUsesBothContainerTypes() {
		try (LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build()) {
			// <figure>
			// container 1 of 2: big
			//   z                         z                         y                         z
			//                             1 +---------------+       1 +---------------+       1 +-------+
			//   | /---------------|   y     |               |         |               |         |       |
			//   |/               /|         |     long      |         |     long      |         | long  |
			// 1 |---------------| | /       |               |         |               |         |       |
			//   |               | |/      0 +---------------+       0 +---------------+       0 +-------+
			//   |     long      | | 1       0               2   x     0               2   x     0       1   y
			//   |               |/
			// 0 |---------------|-- x
			//   0               2
			//
			// container 2 of 2: small
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
			assertUsesBothContainerTypes(packager);
		}
	}

	@Test
	public void bruteForcePackagerUsesBothContainerTypes() {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			// <figure>
			// container 1 of 2: big
			//   z                         z                         y                         z
			//                             1 +---------------+       1 +---------------+       1 +-------+
			//   | /---------------|   y     |               |         |               |         |       |
			//   |/               /|         |     long      |         |     long      |         | long  |
			// 1 |---------------| | /       |               |         |               |         |       |
			//   |               | |/      0 +---------------+       0 +---------------+       0 +-------+
			//   |     long      | | 1       0               2   x     0               2   x     0       1   y
			//   |               |/
			// 0 |---------------|-- x
			//   0               2
			//
			// container 2 of 2: small
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
			assertUsesBothContainerTypes(packager);
		}
	}

	@Test
	public void fastBruteForcePackagerUsesBothContainerTypes() {
		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			// <figure>
			// container 1 of 2: big
			//   z                         z                         y                         z
			//                             1 +---------------+       1 +---------------+       1 +-------+
			//   | /---------------|   y     |               |         |               |         |       |
			//   |/               /|         |     long      |         |     long      |         | long  |
			// 1 |---------------| | /       |               |         |               |         |       |
			//   |               | |/      0 +---------------+       0 +---------------+       0 +-------+
			//   |     long      | | 1       0               2   x     0               2   x     0       1   y
			//   |               |/
			// 0 |---------------|-- x
			//   0               2
			//
			// container 2 of 2: small
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
			assertUsesBothContainerTypes(packager);
		}
	}

	@Test
	public void plainPackagerUsesBothContainerTypesWithSparseGlobalIndexes() {
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			BoxItem longItem = new BoxItem(Box.newBuilder().withId("long").withSize(2, 1, 1).withWeight(1).build(), 1);
			longItem.setGlobalIndex(Integer.MAX_VALUE - 1);
			BoxItem cube = new BoxItem(Box.newBuilder().withId("cube").withSize(1, 1, 1).withWeight(1).build(), 1);
			cube.setGlobalIndex(7);
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containers())
					.withBoxItems(longItem, cube)
					.withMaxContainerCount(2)
					.build();
			// <figure>
			// container 1 of 2: big
			//   z                         z                         y                         z
			//                             1 +---------------+       1 +---------------+       1 +-------+
			//   | /---------------|   y     |               |         |               |         |       |
			//   |/               /|         |     long      |         |     long      |         | long  |
			// 1 |---------------| | /       |               |         |               |         |       |
			//   |               | |/      0 +---------------+       0 +---------------+       0 +-------+
			//   |     long      | | 1       0               2   x     0               2   x     0       1   y
			//   |               |/
			// 0 |---------------|-- x
			//   0               2
			//
			// container 2 of 2: small
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
			figure(result);

			assertThat(result.getContainers()).extracting(Container::getId).containsExactlyInAnyOrder("big", "small");
		}
	}

	private static List<ContainerItem> containers() {
		return List.of(
				new ContainerItem(Container.newBuilder().withId("big").withSize(2, 1, 1).withMaxLoadWeight(10).build(), 1),
				new ContainerItem(Container.newBuilder().withId("small").withSize(1, 1, 1).withMaxLoadWeight(10).build(), 1));
	}

	private static void assertUsesBothContainerTypes(AbstractPackager<?> packager) {
		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(containers())
				.withBoxItems(
						new BoxItem(Box.newBuilder().withId("long").withSize(2, 1, 1).withWeight(1).build(), 1),
						new BoxItem(Box.newBuilder().withId("cube").withSize(1, 1, 1).withWeight(1).build(), 1))
				.withMaxContainerCount(2)
				.build();
		figure(result);

		assertThat(result.getContainers()).extracting(Container::getId).containsExactlyInAnyOrder("big", "small");
	}
}
