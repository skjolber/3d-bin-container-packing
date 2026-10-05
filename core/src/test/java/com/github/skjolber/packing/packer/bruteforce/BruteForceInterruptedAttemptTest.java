package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.ControlledContainerItem;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * An attempt at a container after an interrupted attempt searches from an empty container.
 */
public class BruteForceInterruptedAttemptTest {

	//
	//  container 2 x 1 x 1; boxes a, b (1 x 1 x 1) and c (2 x 1 x 1). At most two boxes fit:
	//
	//   +---+---+
	//   | a | b |
	//   +---+---+
	//
	@Test
	public void bruteForceAttemptsAgainAfterAnInterruptedAttempt() throws PackagerInterruptedException {
		// interrupt the first attempt during the search, after placing the first box
		AtomicInteger checks = new AtomicInteger();
		AtomicInteger interruptAt = new AtomicInteger(3);
		PackagerInterruptSupplier interrupt = () -> checks.incrementAndGet() == interruptAt.get();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			List<BoxItem> boxItems = List.of(
					new BoxItem(Box.newBuilder().withId("a").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 1),
					new BoxItem(Box.newBuilder().withId("b").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 1),
					new BoxItem(Box.newBuilder().withId("c").withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 1));
			Container container = Container.newBuilder().withId("container").withSize(2, 1, 1).withMaxLoadWeight(3).build();
			PackagerAdapter adapter = packager.createBoxItemAdapter(boxItems, new ContainerItemsCalculator(List.of(new ControlledContainerItem(container, 3))), interrupt);

			assertThatThrownBy(() -> adapter.attempt(0, null, false)).isInstanceOf(PackagerInterruptedException.class);

			interruptAt.set(-1);
			assertThat(adapter.attempt(0, null, false).getStack().size()).isEqualTo(2);
		} finally {
			packager.close();
		}
	}
}
