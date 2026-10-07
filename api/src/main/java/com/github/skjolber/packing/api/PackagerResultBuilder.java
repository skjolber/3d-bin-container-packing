package com.github.skjolber.packing.api;

import java.util.List;
import java.util.function.Consumer;

import com.github.skjolber.packing.api.cost.ContainerCostCalculator;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.point.Point;

public interface PackagerResultBuilder {

	public static interface ContainerItemBuilder {

		ContainerItemBuilder withManifestControlsBuilderFactory(ManifestControlsBuilderFactory supplier);

		ContainerItemBuilder withPointControlsBuilderFactory(PointControlsBuilderFactory pointControlsBuilderFactory);

		ContainerItemBuilder withContainerItem(ContainerItem containerItem);
		
		ContainerItemBuilder withContainerItem(Container container, int count);

		default ContainerItemBuilder withCostCalculator(ContainerCostCalculator costCalculator) {
			throw new UnsupportedOperationException("Container cost is not supported by this result builder");
		}
		
		ContainerItemBuilder withPoints(List<Point> points);
		
		ContainerItemBuilder withPoints(Consumer<PointsBuilder> points);

		ContainerItemBuilder withObstacles(Consumer<ObstaclesBuilder> points);

	}
	
	public static interface PointsBuilder {

		PointsBuilder withPoint(Point point);
		
		PointsBuilder withPoint(int x, int y, int z, int dx, int dy, int dz);
	}
	
	public static interface ObstaclesBuilder {

		ObstaclesBuilder withObstacle(Point point);
		
		ObstaclesBuilder withObstacle(int x, int y, int z, int dx, int dy, int dz);
	}
	
	public static interface ControlledBoxItemBuilder {

		ControlledBoxItemBuilder withPointControlsBuilderFactory(PointControlsBuilderFactory pointControlsBuilderFactory);

		ControlledBoxItemBuilder withBoxItem(BoxItem boxItem);
		
		ControlledBoxItemBuilder withBoxItem(Box box, int count);

	}
	
	PackagerResultBuilder withBoxItems(BoxItem... items);

	PackagerResultBuilder withBoxItems(List<BoxItem> items);

	PackagerResultBuilder withOrder(Order order);

	/**
	 * Whether to put the placements of each container in insertion order (see {@link ContainerAccess}), when the box
	 * items have no order. Default true. Skip it when only the outcome matters, for example to check whether an order
	 * fits during checkout; the order can be calculated later, see {@code InsertionSequencer} in {@code core}.
	 *
	 * @param insertionOrder false to keep the placements in the order of the packager's search
	 * @return this builder
	 */
	PackagerResultBuilder withInsertionOrder(boolean insertionOrder);

	PackagerResultBuilder withInterruptDuration(long duration);

	PackagerResultBuilder withInterruptDeadline(long deadline);

	PackagerResultBuilder withInterrupt(PackagerInterruptSupplier interrupt);

	PackagerResultBuilder withMaxContainerCount(int maxResults);

	PackagerResultBuilder withBoxItemGroups(List<BoxItemGroup> items);

	PackagerResultBuilder withBoxItems(BoxItemGroup... items);
	
	PackagerResultBuilder withContainerItems(List<ContainerItem> containers);

	PackagerResultBuilder withContainerItem(Consumer<ContainerItemBuilder> consumer);

	PackagerResultBuilder withContainerItems(ContainerItem... containers);


	/**
	 * 
	 * Build result (perform packaging)
	 * 
	 * @return the result
	 */

	PackagerResult build();

}
