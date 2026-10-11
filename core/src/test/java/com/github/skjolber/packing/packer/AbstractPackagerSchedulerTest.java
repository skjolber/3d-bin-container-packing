package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * The deadline tasks only flag expiry: the scheduler needs one thread, however many packing operations have a deadline.
 */
public class AbstractPackagerSchedulerTest {

	@Test
	void schedulerHasOneThreadAndRemovesCancelledTasks() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			assertThat(packager.scheduledThreadPoolExecutor.getCorePoolSize()).isEqualTo(1);
			assertThat(packager.scheduledThreadPoolExecutor.getRemoveOnCancelPolicy()).isTrue();
		} finally {
			packager.close();
		}
	}

	@Test
	void packingWithDeadlinesReusesOneThreadAndLeavesNoQueuedTasks() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			for(int i = 0; i < 10; i++) {
				PackagerResult result = packager.newResultBuilder()
						.withContainerItems(List.of(new ContainerItem(Container.newBuilder().withId("container").withSize(2, 2, 1).withMaxLoadWeight(100).build(), 1)))
						.withBoxItems(List.of(new BoxItem(Box.newBuilder().withId("box").withSize(1, 1, 1).withWeight(1).build(), 1)))
						.withInterruptDuration(600_000L)
						.build();

				assertThat(result.isSuccess()).isTrue();
			}
			assertThat(packager.scheduledThreadPoolExecutor.getPoolSize()).isEqualTo(1);
			assertThat(packager.scheduledThreadPoolExecutor.getQueue()).isEmpty();

			// an unclosed packager must not block JVM exit
			for (Thread thread : Thread.getAllStackTraces().keySet()) {
				if(thread.getName().equals("packing-packager-deadline")) {
					assertThat(thread.isDaemon()).isTrue();
				}
			}
		} finally {
			packager.close();
		}
	}
}
