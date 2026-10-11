package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.composite.CompositePackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * The library closes only the interrupt suppliers it created: a supplier passed with
 * {@link PackagerResultBuilder#withInterrupt(PackagerInterruptSupplier)} is owned by the caller, so it can be reused.
 */
public class InterruptSupplierOwnershipTest {

	private static final long FAR_FUTURE_MILLIS = 600_000L;

	private static class CountingInterruptSupplier implements PackagerInterruptSupplier {

		private final AtomicInteger closed = new AtomicInteger();
		private final AtomicInteger checks = new AtomicInteger();

		@Override
		public boolean getAsBoolean() {
			checks.incrementAndGet();
			return false;
		}

		@Override
		public void close() {
			closed.incrementAndGet();
		}
	}

	@Test
	void plainPackagerDoesNotCloseUserInterruptWithoutDeadline() throws Exception {
		assertUserInterruptIsNotClosed(() -> PlainPackager.newBuilder().build(), false);
	}

	@Test
	void plainPackagerDoesNotCloseUserInterruptWithDeadline() throws Exception {
		assertUserInterruptIsNotClosed(() -> PlainPackager.newBuilder().build(), true);
	}

	@Test
	void compositePackagerDoesNotCloseUserInterruptWithoutDeadline() throws Exception {
		assertUserInterruptIsNotClosed(InterruptSupplierOwnershipTest::composite, false);
	}

	@Test
	void compositePackagerDoesNotCloseUserInterruptWithDeadline() throws Exception {
		assertUserInterruptIsNotClosed(InterruptSupplierOwnershipTest::composite, true);
	}

	private static CompositePackager composite() {
		return CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(PlainPackager.newBuilder().build(), FAR_FUTURE_MILLIS)
				.build();
	}

	private static <B extends PackagerResultBuilder> void assertUserInterruptIsNotClosed(Supplier<? extends Packager<B>> factory, boolean deadline) throws Exception {
		Packager<B> packager = factory.get();
		try {
			CountingInterruptSupplier interrupt = new CountingInterruptSupplier();

			for(int i = 0; i < 2; i++) {
				PackagerResultBuilder builder = packager.newResultBuilder()
						.withContainerItems(containerItems())
						.withBoxItems(boxItems())
						.withInterrupt(interrupt);
				if(deadline) {
					builder.withInterruptDuration(FAR_FUTURE_MILLIS);
				}
				PackagerResult result = builder.build();

				assertThat(result.isSuccess()).as("pack %d", i).isTrue();
				assertThat(interrupt.closed.get()).as("closes after pack %d", i).isZero();
			}
			assertThat(interrupt.checks.get()).as("the supplier was consulted").isPositive();
		} finally {
			packager.close();
		}
	}

	private static List<ContainerItem> containerItems() {
		return List.of(new ContainerItem(Container.newBuilder().withId("container").withSize(2, 2, 1).withMaxLoadWeight(100).build(), 1));
	}

	private static List<BoxItem> boxItems() {
		return List.of(new BoxItem(Box.newBuilder().withId("box").withSize(1, 1, 1).withWeight(1).build(), 1));
	}
}
