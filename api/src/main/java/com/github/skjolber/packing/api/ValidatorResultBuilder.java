package com.github.skjolber.packing.api;

import java.util.List;
import java.util.function.Consumer;

import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.point.Point;

public interface ValidatorResultBuilder {

	public static interface ControlledContainerItemBuilder {

		ControlledContainerItemBuilder withBoxItemControlsBuilderFactory(ManifestControlsBuilderFactory supplier);

		ControlledContainerItemBuilder withPointControlsBuilderFactory(PointControlsBuilderFactory pointControlsBuilderFactory);

		ControlledContainerItemBuilder withContainerItem(ContainerItem containerItem);
		
		ControlledContainerItemBuilder withContainerItem(Container container, int count);
		
		ControlledContainerItemBuilder withPoints(List<Point> points);
	}
	
	ValidatorResultBuilder withBoxItems(BoxItem... items);

	ValidatorResultBuilder withBoxItems(List<BoxItem> items);

	ValidatorResultBuilder withOrder(Order order);

	ValidatorResultBuilder withMaxContainerCount(int maxResults);

	ValidatorResultBuilder withBoxItemGroups(List<BoxItemGroup> items);

	ValidatorResultBuilder withBoxItems(BoxItemGroup... items);
	
	ValidatorResultBuilder withContainerItems(List<ContainerItem> containers);

	ValidatorResultBuilder withContainerItem(Consumer<ControlledContainerItemBuilder> consumer);

	ValidatorResultBuilder withContainerItems(ContainerItem... containers);


	/**
	 * 
	 * Build result (perform packaging)
	 * 
	 * @return the result
	 */

	ValidatorResult build();

}
