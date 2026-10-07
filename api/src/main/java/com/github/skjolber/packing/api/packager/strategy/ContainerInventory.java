package com.github.skjolber.packing.api.packager.strategy;

import java.util.List;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;
import com.github.skjolber.packing.api.packager.RemainingBoxItemGroup;

/**
 * The containers available to a packaging operation, as seen by a {@link ContainerStrategy}: which
 * container types remain, how many containers can still be used, and whether boxes can be loaded.
 * <p>
 * Container-item indexes refer to {@link #getContainerItems()}.
 */
public interface ContainerInventory {

	/** @return the number of container types */
	int getContainerItemCount();

	ContainerItem getContainerItem(int index);

	List<ContainerItem> getContainerItems();

	/** @return the number of containers which can still be used */
	int getContainerCount();

	/** @return whether the container type can load the box item (dimensions and weight) */
	boolean canLoad(RemainingBoxItem boxItem, int containerItemIndex);

	/** @return whether the container type can load the box item group */
	boolean canLoad(RemainingBoxItemGroup group, int containerItemIndex);

	/**
	 * Return container types which can potentially hold the boxes using the remaining container count.
	 *
	 * @param boxes list of boxes
	 * @return eligible containers and their box-item fit records
	 */
	ContainerItemsResult getContainers(List<RemainingBoxItem> boxes);

	/**
	 * Return container types for a search using at most {@code maxCount} of the remaining containers.
	 *
	 * @param boxes list of boxes
	 * @param maxCount maximum number of containers for this query
	 * @return eligible containers and their box-item fit records
	 */
	ContainerItemsResult getContainers(List<RemainingBoxItem> boxes, int maxCount);

	ContainerItemsResult getGroupContainers(List<RemainingBoxItemGroup> groups);

	/**
	 * Return container types for a search using at most {@code maxCount} of the remaining containers.
	 *
	 * @param groups list of box-item groups
	 * @param maxCount maximum number of containers for this query
	 * @return eligible containers and their group fit records
	 */
	ContainerItemsResult getGroupContainers(List<RemainingBoxItemGroup> groups, int maxCount);

	/**
	 * Return whether the available inventory has enough aggregate volume and weight, and every box-item
	 * type can be loaded by at least one container type.
	 *
	 * @param boxItems remaining box items from this packaging operation
	 * @return {@code true} if every non-empty box item has an available container
	 */
	boolean isFeasible(List<RemainingBoxItem> boxItems);

	/** Check box-item feasibility using at most {@code maxCount} containers. */
	boolean isFeasible(List<RemainingBoxItem> boxItems, int maxCount);

	/**
	 * Check box-item feasibility using at most {@code maxCount} containers while ignoring excluded
	 * container-item indexes.
	 */
	boolean isFeasible(List<RemainingBoxItem> boxItems, int maxCount, boolean[] excluded);

	/**
	 * Return whether the available inventory has enough aggregate volume and weight, and every box-item
	 * group can be loaded by at least one container type.
	 *
	 * @param groups remaining groups from this packaging operation
	 * @return {@code true} if every non-empty group has an available container
	 */
	boolean isGroupFeasible(List<RemainingBoxItemGroup> groups);

	/** Check box-item-group feasibility using at most {@code maxCount} containers. */
	boolean isGroupFeasible(List<RemainingBoxItemGroup> groups, int maxCount);

	/**
	 * Check box-item-group feasibility using at most {@code maxCount} containers while ignoring
	 * excluded container-item indexes.
	 */
	boolean isGroupFeasible(List<RemainingBoxItemGroup> groups, int maxCount, boolean[] excluded);

	/** @return whether at most {@code maxCount} containers can hold a total volume of {@code target} */
	boolean hasMaxVolumeCapacity(int maxCount, long target);

	/** @return whether at most {@code maxCount} containers can hold a total weight of {@code target} */
	boolean hasMaxWeightCapacity(int maxCount, long target);

	/**
	 * @return whether the container types have cost calculators (either none or all have)
	 * @throws IllegalArgumentException if only some container types have a cost calculator
	 */
	boolean hasCost();

	/** @return the total cost of the containers used so far */
	long getCost();
}
