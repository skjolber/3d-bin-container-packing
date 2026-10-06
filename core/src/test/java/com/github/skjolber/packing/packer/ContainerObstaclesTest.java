package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBoxItemBruteForcePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * Obstacles given with the container (rather than with the container item builder) take up space too:
 *
 * <pre>
 *   container 2 x 2 x 1:  [a][b]
 *                         [X][c]     X = obstacle
 * </pre>
 */
public class ContainerObstaclesTest {

	private static Supplier<AbstractPackager<?>> packager(String name) {
		switch (name) {
			case "plain": return () -> PlainPackager.newBuilder().build();
			case "laff": return () -> LargestAreaFitFirstPackager.newBuilder().build();
			case "fastLaff": return () -> FastLargestAreaFitFirstPackager.newBuilder().build();
			case "bruteForce": return () -> BruteForcePackager.newBuilder().build();
			case "fastBruteForce": return () -> FastBruteForcePackager.newBuilder().build();
			case "parallelBruteForce": return () -> ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build();
			default: throw new IllegalArgumentException(name);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff", "bruteForce", "fastBruteForce", "parallelBruteForce" })
	public void packsAroundObstaclesOfTheContainer(String name) {
		Box obstacleBox = Box.newBuilder().withSize(1, 1, 1).withWeight(0).build();
		BoxStackValue obstacleStackValue = obstacleBox.getStackValue(0);
		Placement obstacle = new Placement(obstacleStackValue, -1, 0, 0, 0);

		Container container = Container.newBuilder().withId("c").withSize(2, 2, 1).withMaxLoadWeight(100).build().withObstacles(List.of(obstacle));

		List<BoxItem> boxItems = new ArrayList<>();
		for (String id : List.of("a", "b", "c")) {
			boxItems.add(new BoxItem(Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build(), 1));
		}

		try (AbstractPackager<?> packager = packager(name).get()) {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(List.of(new ContainerItem(container, 2)))
					.withBoxItems(boxItems)
					.withMaxContainerCount(2)
					.withInterruptDuration(10_000)
					.build();

			assertThat(result.isSuccess()).isTrue();
			for (Container packed : result.getContainers()) {
				for (Placement placement : packed.getStack().getPlacements()) {
					assertThat(placement.intersects3D(obstacle)).as("%s overlaps the obstacle", placement.getStackValue().getBox().getId()).isFalse();
				}
			}
		}
	}
}
