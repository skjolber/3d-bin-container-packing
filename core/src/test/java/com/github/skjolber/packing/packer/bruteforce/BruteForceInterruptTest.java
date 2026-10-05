package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.AbstractPackager;

/**
 * Interrupting the brute-force packagers at any point gives a timeout result (or a result found before the
 * interrupt), never an exception. The interrupt fires after a number of checks, from 1 and up, so that it hits every
 * point at which the packagers check it. With four container types, the packagers search for the smallest container
 * which holds all boxes.
 */
public class BruteForceInterruptTest {

	private static final int MAX_CHECKS = 400;

	@Test
	public void bruteForcePackagerCanBeInterruptedAnywhere() {
		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			assertCanBeInterruptedAnywhere(packager);
		} finally {
			packager.close();
		}
	}

	@Test
	public void fastBruteForcePackagerCanBeInterruptedAnywhere() {
		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();
		try {
			assertCanBeInterruptedAnywhere(packager);
		} finally {
			packager.close();
		}
	}

	private static void assertCanBeInterruptedAnywhere(AbstractPackager<?> packager) {
		// one container, or several (then the packagers first search for a single container which holds the boxes)
		for(int maxContainerCount : new int[] {1, 2}) {
			for(int checks = 1; checks <= MAX_CHECKS; checks++) {
				AtomicInteger count = new AtomicInteger();
				int limit = checks;
				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(containers())
						.withBoxItems(boxes())
						.withMaxContainerCount(maxContainerCount)
						.withInterrupt(() -> count.incrementAndGet() > limit)
						.build();

				assertThat(result.isSuccess() || result.isTimeout()).as("max %d containers, interrupted after %d checks", maxContainerCount, checks).isTrue();
			}
		}
	}

	private static List<ContainerItem> containers() {
		return List.of(
				new ContainerItem(container("a", 6, 4), 1),
				new ContainerItem(container("b", 6, 5), 1),
				new ContainerItem(container("c", 7, 5), 1),
				new ContainerItem(container("d", 8, 6), 1));
	}

	/** Squares of total area 24 */
	private static List<BoxItem> boxes() {
		return List.of(new BoxItem(square(3), 2), new BoxItem(square(2), 1), new BoxItem(square(1), 2));
	}

	private static Box square(int size) {
		return Box.newBuilder().withId("square-" + size).withSize(size, size, 1).withRotate3D().withWeight(1).build();
	}

	private static Container container(String id, int dx, int dy) {
		return Container.newBuilder().withId(id).withSize(dx, dy, 1).withMaxLoadWeight(100).build();
	}
}
