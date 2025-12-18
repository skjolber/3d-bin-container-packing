package com.github.skjolber.packing.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.validator.ValidatorResult;
import com.github.skjolber.packing.api.validator.ValidatorResultBuilder;
import com.github.skjolber.packing.api.validator.manifest.ManifestValidatorBuilderFactory;
import com.github.skjolber.packing.api.validator.placement.PlacementValidatorBuilderFactory;

/**
 * {@linkplain ValidatorResult} builder scaffold.
 * 
 */

@SuppressWarnings("unchecked")
public abstract class AbstractValidatorResultBuilder<B extends AbstractValidatorResultBuilder<B>> implements ValidatorResultBuilder {

	protected long deadline = -1L;

	protected BooleanSupplier interrupt;

	protected int maxContainerCount = 1;

	protected Order order = Order.NONE;

	protected List<BoxItemGroup> itemGroups = new ArrayList<>();

	protected List<BoxItem> items = new ArrayList<>();
	
	protected List<ValidatorContainerItem> containers;

	protected PackagerResult packagerResult;

	public static class DefaultValidatorContainerItemBuilder implements ValidatorContainerItemBuilder {

		protected ContainerItem containerItem;
		protected PlacementValidatorBuilderFactory placementValidatorBuilderFactory;
		protected ManifestValidatorBuilderFactory manifestValidatorBuilderFactory;

		@Override
		public ValidatorContainerItemBuilder withManifestValidatorBuilderFactory(
				ManifestValidatorBuilderFactory manifestValidatorBuilderFactory) {
			this.manifestValidatorBuilderFactory = manifestValidatorBuilderFactory;
			return this;
		}

		@Override
		public ValidatorContainerItemBuilder withPlacementValidatorBuilderFactory(
				PlacementValidatorBuilderFactory placementValidatorBuilderFactory) {
			this.placementValidatorBuilderFactory = placementValidatorBuilderFactory;
			return this;
		}

		@Override
		public ValidatorContainerItemBuilder withContainerItem(ContainerItem containerItem) {
			this.containerItem = containerItem;
			return this;
		}
		
		@Override
		public ValidatorContainerItemBuilder withContainerItem(Container container, int count) {
			this.containerItem = new ContainerItem(container, count);
			return this;
		}

		public ValidatorContainerItem build() {
			if (containerItem == null) {
				throw new IllegalStateException("Expected container item");
			}
			ValidatorContainerItem packContainerItem = new ValidatorContainerItem(containerItem);
			packContainerItem.setManifestValidatorBuilderFactory(manifestValidatorBuilderFactory);
			packContainerItem.setPlacementValidatorBuilderFactory(placementValidatorBuilderFactory);
			return packContainerItem;
		}

	}

	public B withContainerItem(Consumer<ValidatorContainerItemBuilder> consumer) {
		DefaultValidatorContainerItemBuilder builder = new DefaultValidatorContainerItemBuilder();
		consumer.accept(builder);
		if (this.containers == null) {
			this.containers = new ArrayList<>();
		}
		this.containers.add(builder.build());
		return (B) this;
	}

	public boolean hasValidator() {
		for (ValidatorContainerItem controlContainerItem : containers) {
			if (controlContainerItem.hasManifestValidatorBuilderFactory()) {
				return true;
			}
			if (controlContainerItem.hasPlacementValidatorBuilderFactory()) {
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
			this.containers.add(new ValidatorContainerItem(item));
		}
		return (B) this;
	}

	@Override
	public B withContainerItems(List<ContainerItem> containers) {
		if (this.containers == null) {
			this.containers = new ArrayList<>(containers.size());
		}
		for (ContainerItem item : containers) {
			this.containers.add(new ValidatorContainerItem(item));
		}
		return (B) this;
	}
	
	@Override
	public B withPackagerResult(PackagerResult packagerResult) {
		this.packagerResult = packagerResult;
		return (B) this;
	}
	
	public B withContainerItem(ContainerItem container) {
		if (this.containers == null) {
			this.containers = new ArrayList<>();
		}
		this.containers.add(new ValidatorContainerItem(container));
		return (B) this;
	}

	public B withBoxItems(BoxItem... items) {
		List<BoxItem> list = new ArrayList<>(items.length);
		for (BoxItem item : items) {
			list.add(item);
		}
		return withBoxItems(list);
	}

	@Override
	public B withBoxItems(List<BoxItem> items) {
		this.items = items;
		return (B) this;
	}

	@Override
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

	@Override
	public B withMaxContainerCount(int maxResults) {
		this.maxContainerCount = maxResults;
		return (B) this;
	}

	protected void validate() {
		if (items != null && !items.isEmpty() && itemGroups != null && !itemGroups.isEmpty()) {
			throw new IllegalStateException("Expected either box items or groups of box items, not both");
		}
		
		if ( (items == null || items.isEmpty()) && (itemGroups == null || itemGroups.isEmpty())) {
			throw new IllegalStateException("Expected either box items or groups of box items");
		}
		
		if (maxContainerCount <= 0) {
			throw new IllegalStateException("Expected one or more max container count");
		}

		if(containers == null || containers.isEmpty()) {
			throw new IllegalStateException("Expected one or more containers");
		}
		
		for (ValidatorContainerItem item : containers) {
			if(item.getCount() == 0) {
				throw new IllegalStateException("Expected one or more count for every container");
			}
		}
								
		if(itemGroups != null) {
			for (BoxItemGroup boxItemGroup : itemGroups) {
				if(boxItemGroup.getId() == null) {
					throw new IllegalStateException("Expected all box item groups to have ids");
				}
				if(boxItemGroup.isEmpty())  {
					throw new IllegalStateException("Expected at least one box in each group");
				}
				
				for (BoxItem boxItem : boxItemGroup.getItems()) {
					if(boxItem.getBox().getId() == null) {
						throw new IllegalStateException("Expected all box items to have ids");
					}
					if(boxItem.getCount() == 0) {
						throw new IllegalStateException("Expected one or more box item count");
					}
																	}
			}
		}
		if(items != null) {
			for (BoxItem item : items) {
				if(item.getBox().getId() == null) {
					throw new IllegalStateException("Expected all box items to have ids");
				}
				if(item.getCount() == 0) {
					throw new IllegalStateException("Expected one or more box item count for " + item.getBox().getId());
				}
			}
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
