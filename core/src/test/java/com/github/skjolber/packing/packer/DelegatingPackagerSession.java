package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerInventory;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;

/**
 * Session which delegates to another session, for tests which change some of its behaviour. Forks and fresh sessions
 * are wrapped with {@link #wrap(PackagerSession)}.
 */
public class DelegatingPackagerSession implements PackagerSession {

	protected final PackagerSession delegate;

	public DelegatingPackagerSession(PackagerSession delegate) {
		this.delegate = delegate;
	}

	protected PackagerSession wrap(PackagerSession session) {
		return new DelegatingPackagerSession(session);
	}

	@Override
	public IntermediatePackagerResult attempt(int containerIndex, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
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
		return wrap(delegate.fresh());
	}

	@Override
	public PackagerSession fork() {
		return wrap(delegate.fork());
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
