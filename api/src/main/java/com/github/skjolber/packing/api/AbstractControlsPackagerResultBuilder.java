package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.github.skjolber.packing.api.packager.BoxItemControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.ControlContainerItem;
import com.github.skjolber.packing.api.packager.PointControlsBuilderFactory;

/**
 * 
 * {@linkplain PackagerResult} builder scaffold.
 * 
 */

@SuppressWarnings("unchecked")
public abstract class AbstractControlsPackagerResultBuilder<B extends AbstractControlsPackagerResultBuilder<B>> extends AbstractPackagerResultBuilder<B> {

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

		public ControlContainerItem build() {
			if(containerItem == null) {
				throw new IllegalStateException("Expected container item");
			}
			ControlContainerItem packContainerItem = new ControlContainerItem(containerItem);
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

		public ControlContainerItem build() {
			if(container == null) {
				throw new IllegalStateException("Expected container item");
			}
			if(count == -1) {
				throw new IllegalStateException("Expected count");
			}
			ControlContainerItem packContainerItem = new ControlContainerItem(container, count);
			packContainerItem.setBoxItemControlsBuilderFactory(boxItemListenerBuilderFactory);
			packContainerItem.setPointControlsBuilderFactory(pointControlsBuilderFactory);
			return packContainerItem;
		}
	}
	
	protected List<ControlContainerItem> containers;

	public B withContainerItems(ContainerItem... containers) {
		if(this.containers == null) {
			this.containers = new ArrayList<>();
		}
		for (ContainerItem item : containers) {
			this.containers.add(new ControlContainerItem(item));
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
			this.containers.add(new ControlContainerItem(item));
		}
		return (B)this;
	}
	
	protected void validate() {
		super.validate();
		if(containers == null || containers.isEmpty()) {
			throw new IllegalStateException();
		}
	}

	public boolean hasControls() {
		for (ControlContainerItem controlContainerItem : containers) {
			if(controlContainerItem.getBoxItemControlsBuilderFactory() != null) {
				return true;
			}
			if(controlContainerItem.getPointControlsBuilderFactory() != null) {
				return true;
			}
		}
		return false;
	}
}
