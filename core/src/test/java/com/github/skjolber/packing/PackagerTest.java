package com.github.skjolber.packing;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.BoxItem;
import com.github.skjolber.packing.BruteForcePackager;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.Packager;
import com.github.skjolber.packing.impl.Adapter;
import com.github.skjolber.packing.impl.PackResult;

class PackagerTest extends AbstractPackagerTest {

	private PackResult incompleteResult;
	private PackResult completeResult;

	static class MyPackager extends Packager {

		private final Adapter adapter;

		MyPackager(List<Container> containers, Adapter adapter) {
			super(containers);

			this.adapter = adapter;
		}

		@Override
		protected Adapter adapter(List<BoxItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
			return adapter;
		}
	}

	@BeforeEach
	void init() {
		incompleteResult = mock(PackResult.class);
		when(incompleteResult.isEmpty()).thenReturn(false);

		completeResult = mock(PackResult.class);
		when(completeResult.isEmpty()).thenReturn(false);
	}

	@Test
	void testBinarySearchChecksBoxesBelowPositive() {

		long deadline = System.currentTimeMillis() + 100000;

		List<Container> containers = new ArrayList<>();

		containers.add(new Container("0", 5, 5, 1, 0));
		containers.add(new Container("1", 5, 5, 1, 0));
		containers.add(new Container("2", 5, 5, 1, 0));
		containers.add(new Container("3", 5, 5, 1, 0));
		containers.add(new Container("4", 5, 5, 1, 0));
		containers.add(new Container("5", 5, 5, 1, 0));
		containers.add(new Container("6", 5, 5, 1, 0));


		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("1", 5, 5, 1, 0), 1));

		Adapter mock = mock(Adapter.class);

		// in the middle first
		when(mock.attempt(eq(3)))
				.thenReturn(completeResult);

		// then in the middle of 0..2
		when(mock.attempt(eq(1)))
				.thenReturn(incompleteResult);

		// then higher
		when(mock.attempt(eq(2)))
				.thenReturn(incompleteResult);

		// then iteration is done for 0...2. Filter out 1 and 2 and try again
		// for 0..0
		when(mock.attempt(eq(0)))
				.thenReturn(incompleteResult);

		when(mock.accepted(any(PackResult.class)))
				.thenReturn(new Container("result", 5, 5, 1, 0));

		when(mock.hasMore(any(PackResult.class))).thenReturn(false, true, true, true);

		MyPackager myPackager = new MyPackager(containers, mock);

		Container pack = myPackager.pack(products, deadline);
		assertEquals("result", pack.getName());
		assertNotNull(pack);
		verify(mock, times(4)).attempt(any(int.class));
	}

	@Test
	void testBinarySearchChecksBoxesBelowPositiveBetterMatch() {

		List<Container> containers = new ArrayList<>();

		containers.add(new Container("0", 5, 5, 1, 0));
		containers.add(new Container("1", 5, 5, 1, 0));
		containers.add(new Container("2", 5, 5, 1, 0));
		containers.add(new Container("3", 5, 5, 1, 0));
		containers.add(new Container("4", 5, 5, 1, 0));
		containers.add(new Container("5", 5, 5, 1, 0));
		containers.add(new Container("6", 5, 5, 1, 0));

		long deadline = System.currentTimeMillis() + 100000;

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("1", 5, 5, 1, 0), 1));

		Adapter mock = mock(Adapter.class);
		when(mock.accepted(completeResult))
				.thenReturn(new Container("final", 5, 5, 1, 0));

		// in the middle first
		when(mock.attempt(eq(3))).thenReturn(completeResult);
		// then in the middle of 0..2
		when(mock.attempt(eq(1))).thenReturn(incompleteResult);
		// then higher
		when(mock.attempt(eq(2))).thenReturn(incompleteResult);

		// then iteration is done for 0...2. Filter out 1 and 2 and try again
		// for 0..0
		when(mock.attempt(eq(0))).thenReturn(completeResult);

		when(mock.hasMore(any(PackResult.class))).thenReturn(false, true, true, false);

		MyPackager myPackager = new MyPackager(containers, mock);

		Container pack = myPackager.pack(products, deadline);
		assertNotNull(pack);
		assertEquals("final", pack.getName());

		verify(mock, times(1)).attempt(eq(3));
		verify(mock, times(1)).attempt(eq(2));
		verify(mock, times(1)).attempt(eq(1));
		verify(mock, times(1)).attempt(eq(0));
	}

	@Test
	void testBinarySearchChecksBoxesTheFirstBox() {

		List<Container> containers = new ArrayList<>();

		containers.add(new Container("0", 5, 5, 1, 0));
		containers.add(new Container("1", 5, 5, 1, 0));
		containers.add(new Container("2", 5, 5, 1, 0));
		containers.add(new Container("3", 5, 5, 1, 0));
		containers.add(new Container("4", 5, 5, 1, 0));
		containers.add(new Container("5", 5, 5, 1, 0));
		containers.add(new Container("6", 5, 5, 1, 0));

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("1", 5, 5, 1, 0), 1));

		Adapter mock = mock(Adapter.class);
		when(mock.hasMore(any(PackResult.class))).thenReturn(false);

		long deadline = System.currentTimeMillis() + 100000;

		PackResult ok = mock(PackResult.class);

		// in the middle first
		when(mock.attempt(eq(3))).thenReturn(ok);

		PackResult better = mock(PackResult.class);

		// then in the middle of 0..2
		when(mock.attempt(eq(1))).thenReturn(better);

		PackResult best = mock(PackResult.class);

		// then lower
		when(mock.attempt(eq(0))).thenReturn(best);

		when(mock.accepted(any(PackResult.class)))
				.thenReturn(new Container("final", 5, 5, 1, 0));

		when(mock.hasMore(any(PackResult.class))).thenReturn(false, false, false);

		MyPackager myPackager = new MyPackager(containers, mock);

		Container pack = myPackager.pack(products, deadline);
		assertNotNull(pack);
		assertEquals("final", pack.getName());

		verify(mock, times(3)).attempt(any(Integer.class));

		verify(mock, times(1)).attempt(eq(3));
		verify(mock, times(1)).attempt(eq(1));
		verify(mock, times(1)).attempt(eq(0));

	}

	@Test
	void testBinarySearchChecksBoxesTheLastBox() {

		long deadline = System.currentTimeMillis() + 100000;

		List<Container> containers = new ArrayList<>();

		containers.add(new Container("0", 5, 5, 1, 0));
		containers.add(new Container("1", 5, 5, 1, 0));
		containers.add(new Container("2", 5, 5, 1, 0));
		containers.add(new Container("3", 5, 5, 1, 0));
		containers.add(new Container("4", 5, 5, 1, 0));
		containers.add(new Container("5", 5, 5, 1, 0));
		containers.add(new Container("6", 5, 5, 1, 0));

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("1", 5, 5, 1, 0), 1));

		Adapter mock = mock(Adapter.class);

		// in the middle first
		when(mock.attempt(eq(3)))
				.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		// then in the middle of 4..6
		when(mock.attempt(eq(5)))
				.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		// then higher
		when(mock.attempt(eq(6)))
				.thenReturn(completeResult).thenThrow(RuntimeException.class);

		// then no more results
		when(mock.attempt(eq(4)))
				.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		// then no more results
		when(mock.attempt(eq(2)))
				.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		// then no more results
		when(mock.attempt(eq(1)))
				.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		// then no more results
		when(mock.attempt(eq(0)))
				.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		when(mock.accepted(any(PackResult.class)))
				.thenReturn(new Container("result", 5, 5, 1, 0));

		when(mock.hasMore(any(PackResult.class))).thenReturn(true, true, false, true, true, true, true);

		MyPackager myPackager = new MyPackager(containers, mock);

		Container pack = myPackager.pack(products, deadline);
		assertNotNull(pack);
		assertEquals("result", pack.getName());

		for (int i = 0; i < containers.size(); i++) {
			verify(mock, times(1)).attempt(eq(i));
		}
	}


	@Test
	void testLimitIsRespected() {

		long deadline = System.currentTimeMillis() + 100000;

		List<Container> containers = new ArrayList<>();

		containers.add(new Container("0", 5, 5, 1, 0));

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("1", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("2", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("3", 5, 5, 1, 0), 1));

		Adapter mock = mock(Adapter.class);

		when(mock.attempt(eq(0))).thenReturn(completeResult);

		when(mock.hasMore(any(PackResult.class))).thenReturn(true, true, false);

		MyPackager myPackager = new MyPackager(containers, mock);

		List<Container> pack = myPackager.packList(products, 2, deadline);
		assertNull(pack);
	}

	// Demonstration on how to run the 2 algorithms in parallel,
	// interrupting the last one to complete
	@Test
	void packagerCanBeInterrupted() throws InterruptedException {
		final Container container = new Container("", 1000, 1500, 3200, 0);
		final List<Container> singleContainer = Collections.singletonList(container);
		final BruteForcePackager bruteForcePackager = new BruteForcePackager(singleContainer);
		final LargestAreaFitFirstPackager largestAreaFitFirstPackager = new LargestAreaFitFirstPackager(container.rotations());

		final AtomicBoolean laffFinished = new AtomicBoolean(false);
		final AtomicBoolean bruteForceFinished = new AtomicBoolean(false);

		new Thread(() -> {
			final Container pack = bruteForcePackager.pack(listOf28Products(), System.currentTimeMillis() + 1000, laffFinished);
			bruteForceFinished.set(pack != null);
		}).start();
		new Thread(() -> {
			final Container pack = largestAreaFitFirstPackager.pack(listOf28Products(), System.currentTimeMillis() + 1000, bruteForceFinished);
			laffFinished.set(pack != null);
		}).start();
		Thread.sleep(100);
		assertTrue(laffFinished.get());
		assertFalse(bruteForceFinished.get());
	}

	@Test
	void packagerCanBeInterruptedWithAFunction() throws InterruptedException {
		final Container container = new Container("", 1000, 1500, 3200, 0);
		final List<Container> singleContainer = Collections.singletonList(container);
		final BruteForcePackager bruteForcePackager = new BruteForcePackager(singleContainer);
		final LargestAreaFitFirstPackager largestAreaFitFirstPackager = new LargestAreaFitFirstPackager(container.rotations());

		final AtomicBoolean laffFinishedBool = new AtomicBoolean(false);
		final AtomicBoolean bruteForceFinishedBool = new AtomicBoolean(false);
		final long start = System.currentTimeMillis();
		BooleanSupplier deadlineReached = () -> System.currentTimeMillis() > start + 1000;

		BooleanSupplier interruptLaff = () -> deadlineReached.getAsBoolean() || bruteForceFinishedBool.get();
		BooleanSupplier interruptBruteForce = () -> deadlineReached.getAsBoolean() || laffFinishedBool.get();

		new Thread(() -> {
			final Container pack = bruteForcePackager.pack(listOf28Products(), interruptBruteForce);
			bruteForceFinishedBool.set(pack != null);
		}).start();
		new Thread(() -> {
			final Container pack = largestAreaFitFirstPackager.pack(listOf28Products(), interruptLaff);
			laffFinishedBool.set(pack != null);
		}).start();
		Thread.sleep(100);
		assertTrue(laffFinishedBool.get());
		assertFalse(bruteForceFinishedBool.get());
	}

}
