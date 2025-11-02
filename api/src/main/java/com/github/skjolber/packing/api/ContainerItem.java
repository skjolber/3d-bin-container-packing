package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 
 * A {@linkplain Container} repeated one or more times.
 * 
 */

public class ContainerItem {

	public static final Comparator<ContainerItem> MAX_LOAD_VOLUME_COMPARATOR = new Comparator<ContainerItem>() {

		@Override
		public int compare(ContainerItem o1, ContainerItem o2) {
			return Long.compare(o1.getContainer().getMaxLoadVolume(), o2.getContainer().getMaxLoadVolume());
		}

	};

	public static final Comparator<ContainerItem> MAX_LOAD_WEIGHT_COMPARATOR = new Comparator<ContainerItem>() {

		@Override
		public int compare(ContainerItem o1, ContainerItem o2) {
			return Long.compare(o1.getContainer().getMaxLoadWeight(), o2.getContainer().getMaxLoadWeight());
		}

	};

	public static Builder newListBuilder() {
		return new Builder();
	}

	public static class Builder {

		private List<ContainerItem> items = new ArrayList<>();

		/**
		 * Add {@linkplain Container}s (note that containers must be added in order of preference).
		 * 
		 * @param containers containers
		 * @return
		 */

		public Builder withContainers(Container... containers) {
			for (Container container : containers) {
				items.add(new ContainerItem(container, Integer.MAX_VALUE));
			}
			return this;
		}

		/**
		 * Add {@linkplain Container}s (note that containers must be added in order of preference).
		 * 
		 * @param containers containers
		 * @return
		 */

		public Builder withContainers(List<Container> containers) {
			for (Container container : containers) {
				items.add(new ContainerItem(container, Integer.MAX_VALUE));
			}
			return this;
		}

		
		/**
		 * Add {@linkplain Container} (note that containers must be added in order of preference).
		 * 
		 * @param container container
		 * @return
		 */
		
		public Builder withContainer(Container container) {
			items.add(new ContainerItem(container, Integer.MAX_VALUE));
			return this;
		}
		
		/**
		 * Add {@linkplain Container} (note that containers must be added in order of preference).
		 * 
		 * @param container container
		 * @param limit max usage of this container
		 * @return
		 */

		public Builder withContainer(Container container, int limit) {
			items.add(new ContainerItem(container, limit));
			return this;
		}

		/**
		 * Add {@linkplain Container}s (note that containers must be added in order of preference).
		 * 
		 * @param containers containers
		 * @param limit max usage (of each individual container)
		 * @return
		 */

		public Builder withContainers(List<Container> containers, int limit) {
			for (Container container : containers) {
				items.add(new ContainerItem(container, limit));
			}
			return this;
		}

		public List<ContainerItem> build() {
			return items;
		}
	}

	private int count;
	private final Container container;
	private int index = -1;

	public ContainerItem(Container container, int count) {
		this.container = container;
		this.count = count;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public Container getContainer() {
		return container;
	}

	public boolean isAvailable() {
		return count > 0;
	}

	public boolean decrement() {
		return decrement(1);
	}

	public boolean decrement(int value) {
		this.count = this.count - value;
		return count > 0;
	}

}
