package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResult;

/**
 * The placement search goes one level deeper per placed box. It must not depend on the thread's stack size.
 *
 * <pre>
 *   [0][1][2][3] ... [4999]     identical unit cubes in a row: a single permutation and rotation
 * </pre>
 */
public class BruteForceDeepSearchTest {

	private static final int BOXES = 5000;

	private static final long STACK_SIZE = 256 * 1024;

	@Test
	public void bruteForcePacksManyBoxesOnASmallThreadStack() throws InterruptedException {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			assertPacksAllBoxes(packager, Box.newBuilder().withId("cube").withSize(1, 1, 1).withWeight(1).build());
		}
	}

	@Test
	public void loadBruteForcePacksManyBoxesOnASmallThreadStack() throws InterruptedException {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			assertPacksAllBoxes(packager, Box.newBuilder().withId("cube").withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(1).build());
		}
	}

	private static void assertPacksAllBoxes(Packager<?> packager, Box box) throws InterruptedException {
		Container container = Container.newBuilder().withId("row").withSize(BOXES, 1, 1).withMaxLoadWeight(BOXES).build();

		AtomicReference<Object> outcome = new AtomicReference<>();
		Thread thread = new Thread(null, () -> {
			try {
				outcome.set(packager.newResultBuilder()
						.withContainerItems(List.of(new ContainerItem(container, 1)))
						.withBoxItems(new BoxItem(box, BOXES))
						.withInterruptDuration(60_000)
						.build());
			} catch(Throwable e) {
				outcome.set(e);
			}
		}, "small-stack", STACK_SIZE);
		thread.start();
		thread.join();

		assertThat(outcome.get()).isInstanceOf(PackagerResult.class);
		PackagerResult result = (PackagerResult)outcome.get();
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.get(0).getStack().size()).isEqualTo(BOXES);
	}
}
