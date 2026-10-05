package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.cost.ContainerCostCalculator;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;

/**
 * {@linkplain PackagerResult} builder scaffold.
 * 
 */

@SuppressWarnings("unchecked")
public abstract class AbstractPackagerResultBuilder<B extends AbstractPackagerResultBuilder<B>> implements PackagerResultBuilder {

	protected long deadline = -1L;

	protected PackagerInterruptSupplier interrupt;

	protected int maxContainerCount = 1;

	protected Order order = Order.NONE;

	protected boolean insertionOrder = true;

	protected List<BoxItemGroup> itemGroups = new ArrayList<>();

	protected List<BoxItem> items = new ArrayList<>();
	
	protected List<ContainerItem> containers;

	public static class DefaultPointsBuilder implements PointsBuilder {
		
		private List<Point> points = new ArrayList<>();

		@Override
		public DefaultPointsBuilder withPoint(Point point) {
			points.add(point);
			return this;
		}

		@Override
		public DefaultPointsBuilder withPoint(int x, int y, int z, int dx, int dy, int dz) {
			points.add(new DefaultPoint3D(x, y, z, x + dx - 1, y + dy - 1, z + dz - 1));
			return this;
		}

		public List<Point> build() {
			if(!points.isEmpty()) {
				return points;
			}
			return Collections.emptyList();
		}
	}
	
	public static class DefaultObstaclesBuilder implements ObstaclesBuilder {
		
		private List<Point> points = new ArrayList<>();

		@Override
		public DefaultObstaclesBuilder withObstacle(Point point) {
			points.add(point);
			return this;
		}

		@Override
		public DefaultObstaclesBuilder withObstacle(int x, int y, int z, int dx, int dy, int dz) {
			points.add(new DefaultPoint3D(x, y, z, x + dx - 1, y + dy - 1, z + dz - 1));
			return this;
		}

		public List<Point> build() {
			if(!points.isEmpty()) {
				return points;
			}
			return Collections.emptyList();
		}
	}
	
	public static class DefaultContainerItemBuilder implements ContainerItemBuilder {

		protected ContainerItem containerItem;
		protected ManifestControlsBuilderFactory boxItemControlsBuilderFactory;
		protected PointControlsBuilderFactory pointControlsBuilderFactory;
		protected List<Point> points;
		protected List<Point> obstacles;
		protected ContainerCostCalculator costCalculator;

		public ContainerItemBuilder withBoxItemControlsBuilderFactory(ManifestControlsBuilderFactory supplier) {
			this.boxItemControlsBuilderFactory = supplier;
			return this;
		}

		public ContainerItemBuilder withPointControlsBuilderFactory(
				PointControlsBuilderFactory pointControlsBuilderFactory) {
			this.pointControlsBuilderFactory = pointControlsBuilderFactory;
			return this;
		}

		public ContainerItemBuilder withContainerItem(ContainerItem containerItem) {
			this.containerItem = containerItem;
			return this;
		}
		
		public ContainerItemBuilder withContainerItem(Container container, int count) {
			this.containerItem = new ContainerItem(container, count);
			return this;
		}

		@Override
		public ContainerItemBuilder withCostCalculator(ContainerCostCalculator costCalculator) {
			this.costCalculator = costCalculator;
			return this;
		}

		public ContainerItem build() {
			if (containerItem == null) {
				throw new IllegalStateException("Expected container item");
			}

			ContainerItem packContainerItem = new ContainerItem(containerItem);

			if(obstacles != null && !obstacles.isEmpty()) {
				if(points != null && !points.isEmpty()) {
					throw new IllegalStateException("Specify either initial points or obstacles, not both");
				}
				// calculate points from obstacles
				Container container = containerItem.getContainer();
				
				DefaultPointCalculator3D ep = new DefaultPointCalculator3D(false, obstacles.size() + 1);
				ep.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
				
				for(int i = 0; i < obstacles.size(); i++) {
					if(!ep.addObstacle(createStackPlacement(obstacles.get(i)))) {
						throw new IllegalStateException("Unable to add obstacle #" + i + " " + obstacles.get(i));
					}
				}
				
				packContainerItem.setInitialPoints(ep.getAll());
			} else {
				packContainerItem.setInitialPoints(points);
			}

			packContainerItem.setBoxItemControlsBuilderFactory(boxItemControlsBuilderFactory);
			packContainerItem.setPointControlsBuilderFactory(pointControlsBuilderFactory);
			if(costCalculator != null) {
				packContainerItem.setCostCalculator(costCalculator);
			}
			return packContainerItem;
		}

		@Override
		public ContainerItemBuilder withPoints(List<Point> points) {
			this.points = points;
			return this;
		}

		@Override
		public ContainerItemBuilder withPoints(Consumer<PointsBuilder> consumer) {
			DefaultPointsBuilder builder = new DefaultPointsBuilder();
			consumer.accept(builder);
			this.points = builder.build();
			return this;
		}
		
		@Override
		public ContainerItemBuilder withObstacles(Consumer<ObstaclesBuilder> consumer) {
			DefaultObstaclesBuilder builder = new DefaultObstaclesBuilder();
			consumer.accept(builder);
			this.obstacles = builder.build();
			return this;
		}
		
		private Placement createStackPlacement(Point point) {
			BoxStackValue stackValue = new BoxStackValue(point.getDx(), point.getDy(), point.getDz(), null, -1);
			
			Box box = Box.newBuilder().withSize(point.getDx(), point.getDy(), point.getDz()).withWeight(0).build();
			stackValue.setBox(box);
			
			return new Placement(stackValue, new DefaultPoint3D(point.getMinX(), point.getMinY(), point.getMinZ(), point.getMaxX(), point.getMaxY(), point.getMaxZ()));
		}
	}

	public B withContainerItem(Consumer<ContainerItemBuilder> consumer) {
		DefaultContainerItemBuilder builder = new DefaultContainerItemBuilder();
		consumer.accept(builder);
		if (this.containers == null) {
			this.containers = new ArrayList<>();
		}
		this.containers.add(builder.build());
		return (B) this;
	}

	public boolean hasControls() {
		for (ContainerItem controlContainerItem : containers) {
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
			this.containers.add(new ContainerItem(item));
		}
		return (B) this;
	}

	public B withContainerItems(List<ContainerItem> containers) {
		if (this.containers == null) {
			this.containers = new ArrayList<>(containers.size());
		}
		for (ContainerItem item : containers) {
			this.containers.add(new ContainerItem(item));
		}
		return (B) this;
	}
	
	public B withContainerItem(ContainerItem container) {
		if (this.containers == null) {
			this.containers = new ArrayList<>();
		}
		this.containers.add(new ContainerItem(container));
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

	@Override
	public B withInsertionOrder(boolean insertionOrder) {
		this.insertionOrder = insertionOrder;
		return (B)this;
	}

	public B withOrder(Order order) {
		this.order = order;
		return (B) this;
	}

	public B withInterruptDeadline(long deadline) {
		this.deadline = deadline;
		return (B) this;
	}

	public B withInterrupt(PackagerInterruptSupplier interrupt) {
		this.interrupt = interrupt;
		return (B) this;
	}

	public B withMaxContainerCount(int maxResults) {
		this.maxContainerCount = maxResults;
		return (B) this;
	}

	/** @return the boxes and containers configured so far */
	protected PackagerInput toInput() {
		return new PackagerInput(items, itemGroups, containers, maxContainerCount, order, insertionOrder);
	}

	/**
	 * Validate the input and check that the packager supports it.
	 *
	 * @param packager the packager
	 * @return the input
	 */
	protected PackagerInput validate(AbstractPackager<?> packager) {
		validate();
		PackagerInput input = toInput();
		String reason = packager.getUnsupportedReason(input);
		if(reason != null) {
			throw new IllegalStateException(reason);
		}
		return input;
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

		for (ContainerItem item : containers) {
			if(item.getCount() == 0) {
				throw new IllegalStateException("Expected one or more count for every container");
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
	
	@Override
	public B withInterruptDuration(long duration) {
		if(duration != -1) {
			return withInterruptDeadline(System.currentTimeMillis() + duration);
		}
		this.deadline = -1;
		return (B)this;
	}
	
}
