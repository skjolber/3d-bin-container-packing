package com.github.skjolber.packing.tornado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;

/**
 * With and without a device, the packager with the accelerator returns the placements of the packager without it: without a TornadoVM runtime the
 * accelerator gives up and the packager searches itself.
 */
class TornadoBruteForceAcceleratorTest {

	private static List<BoxItem> boxItems() {
		List<BoxItem> boxItems = new ArrayList<>();
		boxItems.add(new BoxItem(Box.newBuilder().withId("a").withSize(4, 3, 2).withRotate3D().withWeight(1).build(), 2));
		boxItems.add(new BoxItem(Box.newBuilder().withId("b").withSize(5, 2, 2).withRotate3D().withWeight(1).build(), 2));
		boxItems.add(new BoxItem(Box.newBuilder().withId("c").withSize(3, 3, 3).withRotate3D().withWeight(1).build(), 1));
		return boxItems;
	}

	private static String describe(PackagerResult result) {
		StringBuilder builder = new StringBuilder();
		for (Container container : result.getContainers()) {
			for (Placement placement : container.getStack().getPlacements()) {
				builder.append(placement.getBoxItem().getBox().getId()).append('@').append(placement.getAbsoluteX()).append(',').append(placement.getAbsoluteY()).append(',')
						.append(placement.getAbsoluteZ()).append('+').append(placement.getStackValue().getDx()).append('x').append(placement.getStackValue().getDy()).append('x')
						.append(placement.getStackValue().getDz()).append(' ');
			}
			builder.append("| ");
		}
		return builder.toString();
	}

	private static PackagerResult pack(FastBruteForcePackager packager, List<ContainerItem> containerItems, List<BoxItem> boxItems) {
		return packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(boxItems).withMaxContainerCount(8).withInterruptDuration(60_000).build();
	}

	@Test
	void findingTheDeviceNeverThrows() {
		TornadoBruteForceAccelerator accelerator = TornadoBruteForceAccelerator.newBuilder().build();
		// empty without a TornadoVM runtime, the device of the runtime otherwise
		assertThatCode(accelerator::getDevice).doesNotThrowAnyException();
		assertThat(accelerator.getDevice()).isSameAs(accelerator.getDevice());
	}

	@Test
	void samePlacementsAsThePackagerWithoutAccelerator() {
		TornadoBruteForceAccelerator accelerator = TornadoBruteForceAccelerator.newBuilder().withMinBoxes(1).build();
		FastBruteForcePackager plain = FastBruteForcePackager.newBuilder().build();
		FastBruteForcePackager accelerated = FastBruteForcePackager.newBuilder().withAccelerator(accelerator).build();
		try {
			List<ContainerItem> containerItems = ContainerItem.newListBuilder()
					.withContainer(Container.newBuilder().withId("container").withEmptyWeight(1).withSize(9, 6, 5).withMaxLoadWeight(1000).build(), 2)
					.build();
			String expected = describe(pack(plain, containerItems, boxItems()));
			assertThat(expected).isNotBlank();
			assertThat(describe(pack(accelerated, containerItems, boxItems()))).isEqualTo(expected);

			// random problems, with a second, smaller container, so that the result of the first is a bound for the second
			for (long seed = 1; seed <= 12; seed++) {
				KernelParity.Instance instance = KernelParity.random(seed, 6, 9, 4, 9);
				Container second = Container.newBuilder().withId("second" + seed).withEmptyWeight(1)
						.withSize(Math.max(3, instance.container.getLoadDx() - 1), instance.container.getLoadDy(), Math.max(3, instance.container.getLoadDz() - 1)).withMaxLoadWeight(100_000).build();
				List<ContainerItem> items = ContainerItem.newListBuilder().withContainer(instance.container, 8).withContainer(second, 8).build();
				expected = describe(pack(plain, items, copy(instance.boxItems)));
				assertThat(expected).as("seed " + seed).isNotBlank();
				assertThat(describe(pack(accelerated, items, copy(instance.boxItems)))).as("seed " + seed).isEqualTo(expected);
			}
			assertThat(accelerator.getLastFailure()).isNull();
			if(accelerator.getDevice().isPresent()) {
				assertThat(accelerator.getSearches()).isGreaterThan(12);
			}
		} finally {
			plain.close();
			accelerated.close();
			accelerator.close();
		}
	}

	private static List<BoxItem> copy(List<BoxItem> boxItems) {
		List<BoxItem> result = new ArrayList<>(boxItems.size());
		for (BoxItem boxItem : boxItems) {
			result.add(new BoxItem(boxItem.getBox(), boxItem.getCount()));
		}
		return result;
	}
}
