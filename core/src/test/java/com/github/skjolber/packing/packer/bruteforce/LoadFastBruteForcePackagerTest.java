package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;

class LoadFastBruteForcePackagerTest extends AbstractBruteForcePackagerTest {

	@Override
	protected LoadFastBruteForcePackager createPackager() {
		return LoadFastBruteForcePackager.newBuilder().build();
	}

	@Test
	void rejectsStackWhichExceedsMaxLoadWeight() {
		Box box = Box.newBuilder().withId("box").withSize(10, 10, 1).withWeight(10)
				.withMaxLoadWeight(5).build();

		PackagerResult result = pack(container(2), 2, 2, new BoxItem(box, 2));

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(2);
	}

	@Test
	void rejectsStackWhichExceedsMaxLoadPressure() {
		Box box = Box.newBuilder().withId("box").withSize(2, 5, 1).withWeight(11)
				.withMaxLoadPressure(1).build();

		PackagerResult result = pack(container(2, 5, 2), 2, 2, new BoxItem(box, 2));

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(2);
	}

	@Test
	void enforcesMaxLoadBoxCountAcrossLevels() {
		Box box = Box.newBuilder().withId("box").withSize(10, 10, 1).withWeight(1)
				.withMaxLoadBoxCount(1).build();

		PackagerResult result = pack(container(3), 2, 2, new BoxItem(box, 3));

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(2);
		assertThat(result.getContainers().get(0).getStack()).hasSize(2);
	}

	@Test
	void rejectsDifferentTypesForIdenticalOnlyStacking() {
		Box first = Box.newBuilder().withId("first").withSize(10, 10, 1).withWeight(1)
				.withMaxLoadIdenticalBoxCount(1).build();
		Box second = Box.newBuilder().withId("second").withSize(10, 10, 1).withWeight(1)
				.withMaxLoadIdenticalBoxCount(1).build();

		PackagerResult result = pack(container(2), 2, 2, new BoxItem(first), new BoxItem(second));

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(2);
	}

	@Test
	void recordsLoadAcrossThreeLevels() {
		Box box = Box.newBuilder().withId("box").withSize(10, 10, 1).withWeight(4)
				.withMaxLoadWeight(20).build();

		PackagerResult result = pack(container(3), 1, 1, new BoxItem(box, 3));
		List<Placement> placements = result.getContainers().get(0).getStack().getPlacements().stream()
				.sorted(Comparator.comparingInt(Placement::getAbsoluteZ))
				.toList();

		assertThat(placements).hasSize(3);
		assertThat(placements.get(0).getLoadWeight()).isEqualTo(8);
		assertThat(placements.get(1).getLoadWeight()).isEqualTo(4);
		assertThat(placements.get(2).getLoadWeight()).isZero();
	}

	private static PackagerResult pack(Container container, int containerCount, int maxContainerCount,
			BoxItem... items) {
		try(LoadFastBruteForcePackager packager = LoadFastBruteForcePackager.newBuilder().build()) {
			return packager.newResultBuilder()
					.withContainerItem(new ContainerItem(container, containerCount))
					.withMaxContainerCount(maxContainerCount)
					.withBoxItems(items)
					.build();
		}
	}

	private static Container container(int dz) {
		return Container.newBuilder().withSize(10, 10, dz).withMaxLoadWeight(100).build();
	}

	private static Container container(int dx, int dy, int dz) {
		return Container.newBuilder().withSize(dx, dy, dz).withMaxLoadWeight(100).build();
	}
}
