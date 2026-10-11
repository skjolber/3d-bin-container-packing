package com.github.skjolber.packing.api.packager.strategy;

import java.util.List;
import java.util.function.Supplier;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;

/**
 * Selects a container packing strategy for a packaging operation. Configure with the packager builders'
 * {@code withContainerPackingStrategyFactory(..)}.
 * <p>
 * The packager calls {@link #create(ContainerInventory, List, List, IntermediatePackagerResultComparator, Supplier)} once per
 * packaging operation, with the operation's initial state, and then calls {@link ContainerPackingStrategy#pack} on the
 * returned strategy. Packagers are thread-safe, so the factory is called concurrently when several packaging operations run at
 * the same time. Implementations must be safe for concurrent use; stateless implementations are.
 * <p>
 * Each call returns a strategy for the one packaging operation which its arguments describe: return a new strategy per call
 * (or a stateless one). The packager does not share the returned strategy between packaging operations or sessions, and a
 * factory must not share a strategy which keeps state between calls.
 */
@FunctionalInterface
public interface ContainerPackingStrategyFactory {

	/**
	 * Create the strategy for a packaging operation.
	 * <p>
	 * The comparator and the empty result supplier are the packager's own. Strategies which compare or return results should use
	 * these, so that a custom {@link IntermediatePackagerResultComparator} configured on the packager applies to the strategy too.
	 *
	 * @param inventory the available containers
	 * @param boxItems the remaining box items of the session, or null when packing box item groups. The list belongs to the session
	 *        and changes as containers are accepted: do not modify it
	 * @param boxItemGroups the remaining box item groups of the session, or null when packing box items. The list belongs to the session
	 *        and changes as containers are accepted: do not modify it
	 * @param comparator the packager's result comparator, which ranks the results of packing attempts
	 * @param emptyResultSupplier supplies the packager's empty result, which stands for an attempt which packed no boxes
	 * @return the strategy for this packaging operation
	 */
	ContainerPackingStrategy create(ContainerInventory inventory, List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups,
			IntermediatePackagerResultComparator comparator, Supplier<IntermediatePackagerResult> emptyResultSupplier);
}
