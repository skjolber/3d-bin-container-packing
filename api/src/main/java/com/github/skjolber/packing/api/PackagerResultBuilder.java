package com.github.skjolber.packing.api;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;

public interface PackagerResultBuilder {

	public static interface ControlledContainerItemBuilder {

		ControlledContainerItemBuilder withBoxItemControlsBuilderFactory(ManifestControlsBuilderFactory supplier);

		ControlledContainerItemBuilder withPointControlsBuilderFactory(PointControlsBuilderFactory pointControlsBuilderFactory);

		ControlledContainerItemBuilder withContainerItem(ContainerItem containerItem);
		
		ControlledContainerItemBuilder withContainerItem(Container container, int count);
	}
	
	PackagerResultBuilder withBoxItems(BoxItem... items);

	PackagerResultBuilder withBoxItems(List<BoxItem> items);

	PackagerResultBuilder withPriority(BoxPriority order);

	PackagerResultBuilder withDeadline(long deadline);

	PackagerResultBuilder withInterrupt(BooleanSupplier interrupt);

	PackagerResultBuilder withMaxContainerCount(int maxResults);

	PackagerResultBuilder withBoxItemGroups(List<BoxItemGroup> items);

	PackagerResultBuilder withBoxItems(BoxItemGroup... items);
	
	PackagerResultBuilder withContainerItems(List<ContainerItem> containers);

	PackagerResultBuilder withContainerItem(Consumer<ControlledContainerItemBuilder> consumer);

	PackagerResultBuilder withContainerItems(ContainerItem... containers);


	/**
	 * 
	 * Build result (perform packaging)
	 * 
	 * @return the result
	 */

	PackagerResult build();

}
