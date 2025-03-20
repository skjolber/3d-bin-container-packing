package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * {@linkplain PackagerResult} builder scaffold.
 * 
 */

@SuppressWarnings("unchecked")
public abstract class PackagerResultBuilder<B extends PackagerResultBuilder<B>> {

	protected List<ContainerItem> containers;
	protected List<BoxItem> items;
	protected long deadline = -1L;

	protected BooleanSupplier interrupt;

	protected int maxContainerCount = 1;

	public B withStackables(BoxItem... items) {
		if(this.items == null) {
			this.items = new ArrayList<>();
		}
		for (BoxItem item : items) {
			this.items.add(item);
		}
		return (B)this;
	}

	public B withStackables(List<BoxItem> items) {
		this.items = items;
		return (B)this;
	}

	public B withContainers(ContainerItem... containers) {
		if(this.containers == null) {
			this.containers = new ArrayList<>();
		}
		for (ContainerItem item : containers) {
			this.containers.add(item);
		}
		return (B)this;
	}

	public B withContainers(List<ContainerItem> containers) {
		this.containers = containers;
		return (B)this;
	}

	public B withDeadline(long deadline) {
		this.deadline = deadline;
		return (B)this;
	}

	public B withInterrupt(BooleanSupplier interrupt) {
		this.interrupt = interrupt;
		return (B)this;
	}

	public B withMaxContainerCount(int maxResults) {
		this.maxContainerCount = maxResults;
		return (B)this;
	}

	/**
	 * 
	 * Build result (perform packaging)
	 * 
	 * @return the result
	 */

	public abstract PackagerResult build();

}
