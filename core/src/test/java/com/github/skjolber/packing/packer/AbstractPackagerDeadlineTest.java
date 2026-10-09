package com.github.skjolber.packing.packer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * Deadline checks must not leak scheduler threads or queued tasks.
 */

public class AbstractPackagerDeadlineTest extends AbstractPackagerTest {

	@Test
	void testRepeatedPackWithDeadlineDoesNotLeakSchedulerThreads() throws Exception {
		Container container = Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(2, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build();
		ContainerItem containerItem = new ContainerItem(container, 1);

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			ScheduledThreadPoolExecutor executor = packager.getScheduledThreadPoolExecutor();

			for (int i = 0; i < 10; i++) {
				List<BoxItem> products = new ArrayList<>();
				products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 3));

				// far-future deadline: the deadline task is scheduled, but never runs
				PackagerResult result = packager.newResultBuilder()
						.withContainerItem(containerItem)
						.withBoxItems(products)
						.withDeadline(System.currentTimeMillis() + 3_600_000L)
						.build();
				assertValid(result);

				// the cancelled deadline task is removed, and no further threads were started
				assertTrue(executor.getPoolSize() <= 1, "Pool size " + executor.getPoolSize() + " after pack " + i);
				assertTrue(executor.getQueue().isEmpty(), "Queue size " + executor.getQueue().size() + " after pack " + i);
			}

			// a long-lived scheduler thread must never keep the JVM alive
			assertTrue(executor.submit(() -> Thread.currentThread().isDaemon()).get());
		} finally {
			packager.close();
		}
		assertTrue(packager.getScheduledThreadPoolExecutor().isShutdown());
	}
}
