package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;

/** Calculates a minimum container cost for regular box items or groups. */
public interface ContainerItemsCostCalculator {

	/**
	 * @param containers current container inventory
	 * @return the cost, or {@link Long#MAX_VALUE} if the items cannot fit
	 */
	long getMinimumCost(ContainerItemsCalculator containers, List<BoxItem> boxes, int maxCount);

	/**
	 * @param containers current container inventory
	 * @return the cost, or {@link Long#MAX_VALUE} if the groups cannot fit
	 */
	long getGroupMinimumCost(ContainerItemsCalculator containers, List<BoxItemGroup> groups, int maxCount);
}
