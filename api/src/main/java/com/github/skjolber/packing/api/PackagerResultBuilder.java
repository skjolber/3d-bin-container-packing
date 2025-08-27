package com.github.skjolber.packing.api;

import java.util.List;
import java.util.function.BooleanSupplier;

public interface PackagerResultBuilder<B extends PackagerResultBuilder<B>> {

	/**
	 * 
	 * Build result (perform packaging)
	 * 
	 * @return the result
	 */

	PackagerResult build();

	B withBoxItems(BoxItem... items);

	B withBoxItems(List<BoxItem> items);

	B withPriority(BoxPriority order);

	B withDeadline(long deadline);

	B withInterrupt(BooleanSupplier interrupt);

	B withMaxContainerCount(int maxResults);

	B withBoxItemGroups(List<BoxItemGroup> items);

	B withBoxItems(BoxItemGroup... items);
	
	B withContainerItems(List<ContainerItem> containers);
}
