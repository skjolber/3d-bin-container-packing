package com.github.skjolber.packing.packer.composite;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerInventory;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.packer.PackagerInput;
import com.github.skjolber.packing.packer.bruteforce.BruteForceIntermediatePackagerResultComparator;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;

/**
 * Fast brute-force packager which counts its sessions' attempts. Before each attempt, it runs a hook, which can
 * throw {@link PackagerInterruptedException}.
 */
class CountingFastBruteForcePackager extends FastBruteForcePackager {

	@FunctionalInterface
	interface AttemptHook {
		void beforeAttempt() throws PackagerInterruptedException;
	}

	private final AtomicInteger attempts = new AtomicInteger();
	private final AttemptHook hook;

	CountingFastBruteForcePackager() {
		this(() -> {});
	}

	CountingFastBruteForcePackager(AttemptHook hook) {
		super(new BruteForceIntermediatePackagerResultComparator(), DEFAULT_POINT_COMPARATOR);
		this.hook = hook;
	}

	int getAttempts() {
		return attempts.get();
	}

	@Override
	protected PackagerSession newSession(PackagerInput input, PackagerInterruptSupplier interrupt) {
		return new CountingSession(super.newSession(input, interrupt));
	}

	private class CountingSession implements PackagerSession {

		private final PackagerSession delegate;

		CountingSession(PackagerSession delegate) {
			this.delegate = delegate;
		}

		@Override
		public IntermediatePackagerResult attempt(int containerIndex, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			attempts.incrementAndGet();
			hook.beforeAttempt();
			return delegate.attempt(containerIndex, best, abortOnAnyBoxTooBig);
		}

		@Override
		public IntermediatePackagerResult peek(int containerIndex, IntermediatePackagerResult existing) {
			return delegate.peek(containerIndex, existing);
		}

		@Override
		public Container accept(IntermediatePackagerResult result) {
			return delegate.accept(result);
		}

		@Override
		public List<Integer> getContainers() {
			return delegate.getContainers();
		}

		@Override
		public PackagerSession fresh() {
			return new CountingSession(delegate.fresh());
		}

		@Override
		public PackagerSession fork() {
			return new CountingSession(delegate.fork());
		}

		@Override
		public long getRemainingVolume() {
			return delegate.getRemainingVolume();
		}

		@Override
		public long getRemainingWeight() {
			return delegate.getRemainingWeight();
		}

		@Override
		public ContainerInventory getContainerInventory() {
			return delegate.getContainerInventory();
		}

		@Override
		public List<BoxItem> getRemainingBoxItems() {
			return delegate.getRemainingBoxItems();
		}

		@Override
		public List<BoxItemGroup> getRemainingBoxItemGroups() {
			return delegate.getRemainingBoxItemGroups();
		}

		@Override
		public ContainerItem getContainerItem(int index) {
			return delegate.getContainerItem(index);
		}

		@Override
		public int countRemainingBoxes() {
			return delegate.countRemainingBoxes();
		}

		@Override
		public int countRemainingBoxItemGroups() {
			return delegate.countRemainingBoxItemGroups();
		}

		@Override
		public int getMaxContainerCount() {
			return delegate.getMaxContainerCount();
		}
	}
}
