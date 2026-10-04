package com.github.skjolber.packing.api.packager.strategy;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;

/**
 * One packaging operation as seen by a {@link ContainerStrategy}: the remaining boxes and the
 * available containers. A strategy repeatedly tries to pack the remaining boxes into containers
 * ({@link #attempt(int, IntermediatePackagerResult, boolean)}) and keeps the results it prefers
 * ({@link #accept(IntermediatePackagerResult)}), until no boxes remain or no container can be used.
 * <p>
 * Container indexes refer to {@link ContainerInventory#getContainerItems()}.
 */
public interface PackagerSession {

	/**
	 * Pack as many of the remaining boxes as possible into a container. The session is not changed.
	 *
	 * @param containerIndex the container type
	 * @param best the best result so far, or null. If not null, the session may return an empty result instead of a
	 *        result with less load volume than {@code best} (see {@link IntermediatePackagerResult#getLoadVolume()})
	 * @param abortOnAnyBoxTooBig whether to give up (return an empty result) if some remaining box does not fit the container
	 * @return the result, possibly empty
	 * @throws PackagerInterruptedException if the packaging operation is interrupted
	 */
	IntermediatePackagerResult attempt(int containerIndex, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException;

	/**
	 * Reuse an existing result for another container type without packing again, if the packed boxes
	 * fit inside it unchanged.
	 *
	 * @return a result for the container type, or null if the existing result cannot be reused
	 */
	IntermediatePackagerResult peek(int containerIndex, IntermediatePackagerResult existing);

	/**
	 * Accept a result: its boxes are removed from the remaining boxes and its container is used.
	 * The result may originate from another session of the same packaging operation (for example a
	 * {@link #fork()}).
	 *
	 * @return the packed container
	 */
	Container accept(IntermediatePackagerResult result);

	/** @return the indexes of the container types which can potentially hold the remaining boxes */
	List<Integer> getContainers();

	/** Creates an independent session at the start of the same packaging operation. */
	PackagerSession fresh();

	/**
	 * Creates an independent session at the current packing state. The caller may accept a container
	 * on the fork without changing this session.
	 */
	PackagerSession fork();

	long getRemainingVolume();

	long getRemainingWeight();

	ContainerInventory getContainerInventory();

	/** @return the remaining box items, or null when packing box item groups */
	List<BoxItem> getRemainingBoxItems();

	/** @return the remaining box item groups, or null when packing box items */
	List<BoxItemGroup> getRemainingBoxItemGroups();

	ContainerItem getContainerItem(int index);

	int countRemainingBoxes();

	int countRemainingBoxItemGroups();

	/**
	 * @return the number of containers which can still be used for the remaining boxes: the remaining
	 *         container count, but at most one container per remaining box (or box item group)
	 */
	int getMaxContainerCount();
}
