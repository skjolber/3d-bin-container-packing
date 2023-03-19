package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * A {@linkplain Container} repeated one or more times.
 * 
 */

public class ContainerItem {

	public static Builder newListBuilder() {
		return new Builder();
	}

	public static class Builder {

		private List<ContainerItem> items = new ArrayList<>();

		public Builder withContainers(Container... containers) {
			for (Container container : containers) {
				items.add(new ContainerItem(container, Integer.MAX_VALUE));
			}
			return this;
		}

		public Builder withContainers(List<Container> containers) {
			for (Container container : containers) {
				items.add(new ContainerItem(container, Integer.MAX_VALUE));
			}
			return this;
		}

		public Builder withContainer(Container container) {
			items.add(new ContainerItem(container, Integer.MAX_VALUE));
			return this;
		}

		public Builder withContainer(Container container, int limit) {
			items.add(new ContainerItem(container, limit));
			return this;
		}

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

	public ContainerItem(Container container, int count) {
		this.container = container;
		this.count = count;
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

	public void consume() {
		count--;
	}

}
