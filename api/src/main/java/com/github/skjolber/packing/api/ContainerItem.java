package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.cost.ContainerCostCalculator;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.point.Point;

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
		 * Add a container with a usage limit and cost calculator.
		 *
		 * @param container container
		 * @param limit max usage of this container
		 * @param costCalculator cost calculator for one use of the container
		 * @return this builder
		 */
		public Builder withContainer(Container container, int limit, ContainerCostCalculator costCalculator) {
			items.add(new ContainerItem(container, limit, costCalculator));
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
	private int resetCount;
	private final Container container;
	private int index = -1;
	
	protected ManifestControlsBuilderFactory manifestControlsBuilderFactory;
	protected PointControlsBuilderFactory pointControlsBuilderFactory;
	protected List<Point> initialPoints; 
	protected ContainerCostCalculator costCalculator;

	public ContainerItem(Container container, int count) {
		this(container, count, null);
	}

	public ContainerItem(Container container, int count, ContainerCostCalculator costCalculator) {
		this.container = container;
		this.count = count;
		this.resetCount = count;
		this.costCalculator = costCalculator;
	}

	public ContainerItem(ContainerItem containerItem) {
		this.container = containerItem.container;
		this.count = containerItem.count;
		this.resetCount = containerItem.resetCount;
		this.index = containerItem.index;
		this.manifestControlsBuilderFactory = containerItem.manifestControlsBuilderFactory;
		this.pointControlsBuilderFactory = containerItem.pointControlsBuilderFactory;
		this.initialPoints = containerItem.initialPoints;
		this.costCalculator = containerItem.costCalculator;
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

	public void reset() {
		this.count = resetCount;
	}

	public void setResetCount(int resetCount) {
		this.resetCount = resetCount;
	}

	public void decrementResetCount() {
		this.resetCount--;
	}

	public void mark() {
		this.resetCount = count;
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
	
	public void setInitialPoints(List<Point> points) {
		this.initialPoints = points;
	}
	
	public List<Point> getInitialPoints() {
		return initialPoints;
	}

	public void setPointControlsBuilderFactory(PointControlsBuilderFactory pointControlsBuilderFactory) {
		this.pointControlsBuilderFactory = pointControlsBuilderFactory;
	}
	
	public PointControlsBuilderFactory getPointControlsBuilderFactory() {
		return pointControlsBuilderFactory;
	}
	
	public void setBoxItemControlsBuilderFactory(ManifestControlsBuilderFactory factory) {
		this.manifestControlsBuilderFactory = factory;
	}
	
	public ManifestControlsBuilderFactory getBoxItemControlsBuilderFactory() {
		return manifestControlsBuilderFactory;
	}

	public boolean hasPointControlsBuilderFactory() {
		return pointControlsBuilderFactory != null;
	}
	
	public boolean hasBoxItemControlsBuilderFactory() {
		return manifestControlsBuilderFactory != null;
	}	
	
	public boolean hasControls() {
		return hasPointControlsBuilderFactory() || hasBoxItemControlsBuilderFactory();
	}
	
	public boolean hasInitialPoints() {
		return initialPoints != null && !initialPoints.isEmpty();
	}

	public ContainerCostCalculator getCostCalculator() {
		return costCalculator;
	}

	public void setCostCalculator(ContainerCostCalculator costCalculator) {
		this.costCalculator = costCalculator;
	}

	public boolean hasCostCalculator() {
		return costCalculator != null;
	}

}
