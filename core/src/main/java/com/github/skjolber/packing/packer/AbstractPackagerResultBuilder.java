package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.point.Point;

/**
 * {@linkplain PackagerResult} builder scaffold.
 * 
 */

@SuppressWarnings("unchecked")
public abstract class AbstractPackagerResultBuilder<B extends AbstractPackagerResultBuilder<B>> implements PackagerResultBuilder {

	protected long deadline = -1L;

	protected BooleanSupplier interrupt;

	protected int maxContainerCount = 1;

	protected Order order = Order.NONE;

	protected List<BoxItemGroup> itemGroups = new ArrayList<>();

	protected List<BoxItem> items = new ArrayList<>();
	
	protected List<ControlledContainerItem> containers;

	public static class DefaultControlledContainerItemBuilder implements ControlledContainerItemBuilder {

		protected ContainerItem containerItem;
		protected ManifestControlsBuilderFactory boxItemControlsBuilderFactory;
		protected PointControlsBuilderFactory pointControlsBuilderFactory;
		protected List<Point> points;

		public ControlledContainerItemBuilder withBoxItemControlsBuilderFactory(ManifestControlsBuilderFactory supplier) {
			this.boxItemControlsBuilderFactory = supplier;
			return this;
		}

		public ControlledContainerItemBuilder withPointControlsBuilderFactory(
				PointControlsBuilderFactory pointControlsBuilderFactory) {
			this.pointControlsBuilderFactory = pointControlsBuilderFactory;
			return this;
		}

		public ControlledContainerItemBuilder withContainerItem(ContainerItem containerItem) {
			this.containerItem = containerItem;
			return this;
		}
		
		public ControlledContainerItemBuilder withContainerItem(Container container, int count) {
			this.containerItem = new ContainerItem(container, count);
			return this;
		}

		public ControlledContainerItem build() {
			if (containerItem == null) {
				throw new IllegalStateException("Expected container item");
			}
			ControlledContainerItem packContainerItem = new ControlledContainerItem(containerItem);
			packContainerItem.setBoxItemControlsBuilderFactory(boxItemControlsBuilderFactory);
			packContainerItem.setPointControlsBuilderFactory(pointControlsBuilderFactory);
			packContainerItem.setInitialPoints(points);
			return packContainerItem;
		}

		@Override
		public ControlledContainerItemBuilder withPoints(List<Point> points) {
			this.points = points;
			return this;
		}
	}

	public B withContainerItem(Consumer<ControlledContainerItemBuilder> consumer) {
		DefaultControlledContainerItemBuilder builder = new DefaultControlledContainerItemBuilder();
		consumer.accept(builder);
		if (this.containers == null) {
			this.containers = new ArrayList<>();
		}
		this.containers.add(builder.build());
		return (B) this;
	}

	public boolean hasControls() {
		for (ControlledContainerItem controlContainerItem : containers) {
			if (controlContainerItem.hasPointControlsBuilderFactory()) {
				return true;
			}
			if (controlContainerItem.hasBoxItemControlsBuilderFactory()) {
				return true;
			}
		}
		return false;
	}	
	
	public B withContainerItems(ContainerItem... containers) {
		if (this.containers == null) {
			this.containers = new ArrayList<>(containers.length);
		}
		for (ContainerItem item : containers) {
			this.containers.add(new ControlledContainerItem(item));
		}
		return (B) this;
	}

	public B withContainerItems(List<ContainerItem> containers) {
		if (this.containers == null) {
			this.containers = new ArrayList<>(containers.size());
		}
		for (ContainerItem item : containers) {
			this.containers.add(new ControlledContainerItem(item));
		}
		return (B) this;
	}
	
	public B withContainerItem(ContainerItem container) {
		if (this.containers == null) {
			this.containers = new ArrayList<>();
		}
		this.containers.add(new ControlledContainerItem(container));
		return (B) this;
	}

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

	public B withOrder(Order order) {
		this.order = order;
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
	
}
