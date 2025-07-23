package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.skjolber.packing.api.ep.FilteredPointsBuilder;
import com.github.skjolber.packing.api.packager.BoxItemControlsBuilder;
import com.github.skjolber.packing.api.packager.BoxItemControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.CompositeContainerItem;
import com.github.skjolber.packing.api.packager.PointControlsBuilderFactory;

/**
 * {@linkplain PackagerResult} builder scaffold.
 * 
 */

@SuppressWarnings("unchecked")
public abstract class AbstractPackagerResultBuilder<B extends AbstractPackagerResultBuilder<B>> {

	public static class ExtendedContainerItemBuilder {
		
		protected ContainerItem containerItem;
		protected BoxItemControlsBuilderFactory boxItemControlsBuilderFactory;
		protected PointControlsBuilderFactory pointControlsBuilderFactory;
		
		public ExtendedContainerItemBuilder withBoxItemControlsBuilderFactory(BoxItemControlsBuilderFactory supplier) {
			this.boxItemControlsBuilderFactory = supplier;
			return this;
		}
		
		public ExtendedContainerItemBuilder withPointControlsBuilderFactory(PointControlsBuilderFactory pointControlsBuilderFactory) {
			this.pointControlsBuilderFactory = pointControlsBuilderFactory;
			return this;
		}
		
		public ExtendedContainerItemBuilder withContainerItem(ContainerItem containerItem) {
			this.containerItem = containerItem;
			return this;
		}

		public CompositeContainerItem build() {
			if(containerItem == null) {
				throw new IllegalStateException("Expected container item");
			}
			CompositeContainerItem packContainerItem = new CompositeContainerItem(containerItem);
			packContainerItem.setBoxItemControlsBuilderFactory(boxItemControlsBuilderFactory);
			packContainerItem.setPointControlsBuilderFactory(pointControlsBuilderFactory);
			return packContainerItem;
		}
	}
	
	public static class ExtendedContainerBuilder {
		
		protected Container container;
		protected int count = -1;
		
		protected BoxItemControlsBuilderFactory boxItemListenerBuilderFactory;
		protected PointControlsBuilderFactory pointControlsBuilderFactory;

		public ExtendedContainerBuilder withBoxItemListenerBuilderSupplier(BoxItemControlsBuilderFactory factory) {
			this.boxItemListenerBuilderFactory = factory;
			return this;
		}
		
		public ExtendedContainerBuilder withPointControlsBuilderFactory(PointControlsBuilderFactory pointControlsBuilderFactory) {
			this.pointControlsBuilderFactory = pointControlsBuilderFactory;
			return this;
		}
		
		public ExtendedContainerBuilder withContainer(Container container) {
			this.container = container;
			return this;
		}

		public CompositeContainerItem build() {
			if(container == null) {
				throw new IllegalStateException("Expected container item");
			}
			if(count == -1) {
				throw new IllegalStateException("Expected count");
			}
			ContainerItem containerItem = new ContainerItem(container, count);
			CompositeContainerItem packContainerItem = new CompositeContainerItem(containerItem);
			packContainerItem.setBoxItemControlsBuilderFactory(boxItemListenerBuilderFactory);
			packContainerItem.setPointControlsBuilderFactory(pointControlsBuilderFactory);
			return packContainerItem;
		}
	}
	
	protected List<CompositeContainerItem> containers;
	protected long deadline = -1L;

	protected BooleanSupplier interrupt;

	protected int maxContainerCount = 1;
	
	protected Priority priority = Priority.NONE;
	
	protected Supplier<BoxItemControlsBuilder<?>> boxItemListenerBuilderSupplier;
	protected Supplier<FilteredPointsBuilder<?>> filteredPointsBuilderSupplier;
	
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
		return (B)this;
	}

	public B withPriority(Priority order) {
		this.priority = order;
		return (B)this;
	}

	public B withContainerItems(ContainerItem... containers) {
		if(this.containers == null) {
			this.containers = new ArrayList<>();
		}
		for (ContainerItem item : containers) {
			this.containers.add(new CompositeContainerItem(item));
		}
		return (B)this;
	}
	
	public B withContainerItem(Consumer<ExtendedContainerItemBuilder> consumer) {
		ExtendedContainerItemBuilder builder = new ExtendedContainerItemBuilder();
		consumer.accept(builder);
		if(this.containers == null) {
			this.containers = new ArrayList<>();
		}
		this.containers.add(builder.build());
		return (B)this;
	}
	
	public B withContainer(Consumer<ExtendedContainerBuilder> consumer) {
		ExtendedContainerBuilder builder = new ExtendedContainerBuilder();
		consumer.accept(builder);
		if(this.containers == null) {
			this.containers = new ArrayList<>();
		}
		this.containers.add(builder.build());
		return (B)this;
	}

	public B withContainerItems(List<ContainerItem> containers) {
		if(this.containers == null) {
			this.containers = new ArrayList<>();
		}
		for (ContainerItem item : containers) {
			this.containers.add(new CompositeContainerItem(item));
		}
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
	
	protected void validate() {
		if(items != null && !items.isEmpty() && itemGroups != null && !itemGroups.isEmpty()) {
			throw new IllegalStateException("Expected either box items or groups of box items, not both");
		}
		if(maxContainerCount <= 0) {
			throw new IllegalStateException();
		}
		if(containers == null) {
			throw new IllegalStateException();
		}
	}

	/**
	 * 
	 * Build result (perform packaging)
	 * 
	 * @return the result
	 */

	public abstract PackagerResult build();

	public B withBoxItemGroups(List<BoxItemGroup> items) {
		this.itemGroups = items;
		return (B)this;
	}

	public B withBoxItems(BoxItemGroup... items) {
		List<BoxItemGroup> list = new ArrayList<>(items.length);
		for (BoxItemGroup item : items) {
			list.add(item);
		}
		return withBoxItemGroups(list);
	}
}
