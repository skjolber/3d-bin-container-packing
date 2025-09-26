package com.github.skjolber.packing.api;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;

public interface PackagerResultBuilder<B extends PackagerResultBuilder<B>> {

	public static interface ControlledContainerItemBuilder {

		ControlledContainerItemBuilder withBoxItemControlsBuilderFactory(ManifestControlsBuilderFactory supplier);

		ControlledContainerItemBuilder withPointControlsBuilderFactory(PointControlsBuilderFactory pointControlsBuilderFactory);

		ControlledContainerItemBuilder withContainerItem(ContainerItem containerItem);
		
		ControlledContainerItemBuilder withContainerItem(Container container, int count);
	}
	
	B withBoxItems(BoxItem... items);

	B withBoxItems(List<BoxItem> items);

	B withPriority(BoxPriority order);

	B withDeadline(long deadline);

	B withInterrupt(BooleanSupplier interrupt);

	B withMaxContainerCount(int maxResults);

	B withBoxItemGroups(List<BoxItemGroup> items);

	B withBoxItems(BoxItemGroup... items);
	
	B withContainerItems(List<ContainerItem> containers);

	B withContainerItem(Consumer<ControlledContainerItemBuilder> consumer);

	B withContainerItems(ContainerItem... containers);


	/**
	 * 
	 * Build result (perform packaging)
	 * 
	 * @return the result
	 */

	PackagerResult build();

}
