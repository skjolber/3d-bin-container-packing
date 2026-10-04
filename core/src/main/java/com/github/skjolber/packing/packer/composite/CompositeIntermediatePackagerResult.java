package com.github.skjolber.packing.packer.composite;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;

/**
 * A result of one of the packagers of a {@link CompositePackagerSession}.
 */
public class CompositeIntermediatePackagerResult implements IntermediatePackagerResult {

	protected final IntermediatePackagerResult delegate;
	protected final int stage;

	public CompositeIntermediatePackagerResult(IntermediatePackagerResult delegate, int stage) {
		this.delegate = delegate;
		this.stage = stage;
	}

	/** @return the packager's own result */
	public IntermediatePackagerResult getDelegate() {
		return delegate;
	}

	/** @return the index of the packager which produced the result */
	public int getStage() {
		return stage;
	}

	@Override
	public ContainerItem getContainerItem() {
		return delegate.getContainerItem();
	}

	@Override
	public Stack getStack() {
		return delegate.getStack();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public long getLoadVolume() {
		return delegate.getLoadVolume();
	}
}
