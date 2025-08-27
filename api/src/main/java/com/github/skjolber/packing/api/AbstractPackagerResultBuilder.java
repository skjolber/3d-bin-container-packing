package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * {@linkplain PackagerResult} builder scaffold.
 * 
 */

@SuppressWarnings("unchecked")
public abstract class AbstractPackagerResultBuilder<B extends AbstractPackagerResultBuilder<B>>
		implements PackagerResultBuilder<B> {

	protected long deadline = -1L;

	protected BooleanSupplier interrupt;

	protected int maxContainerCount = 1;

	protected BoxPriority priority = BoxPriority.NONE;

	protected List<BoxItemGroup> itemGroups = new ArrayList<>();

	protected List<BoxItem> items = new ArrayList<>();

	public B withBoxItems(BoxItem... items) {
		List<BoxItem> list = new ArrayList<>(items.length);
		for (BoxItem item : items) {
			list.add(item);
		}
		return withBoxItems(list);
	}

	public B withBoxItems(List<BoxItem> items) {
		this.items = items;
		return (B) this;
	}

	public B withPriority(BoxPriority order) {
		this.priority = order;
		return (B) this;
	}

	public B withDeadline(long deadline) {
		this.deadline = deadline;
		return (B) this;
	}

	public B withInterrupt(BooleanSupplier interrupt) {
		this.interrupt = interrupt;
		return (B) this;
	}

	public B withMaxContainerCount(int maxResults) {
		this.maxContainerCount = maxResults;
		return (B) this;
	}

	protected void validate() {
		if (items != null && !items.isEmpty() && itemGroups != null && !itemGroups.isEmpty()) {
			throw new IllegalStateException("Expected either box items or groups of box items, not both");
		}
		if (maxContainerCount <= 0) {
			throw new IllegalStateException();
		}
	}

	public B withBoxItemGroups(List<BoxItemGroup> items) {
		this.itemGroups = items;
		return (B) this;
	}

	public B withBoxItems(BoxItemGroup... items) {
		List<BoxItemGroup> list = new ArrayList<>(items.length);
		for (BoxItemGroup item : items) {
			list.add(item);
		}
		return withBoxItemGroups(list);
	}
	
	public abstract B withContainerItems(List<ContainerItem> containers);
}
