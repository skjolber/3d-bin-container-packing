package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.InsertionOrder;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;

/**
 * Reorders the placements of a packed container into a possible insertion order (see {@link InsertionOrder}):
 * each box after the boxes it rests on, and after the boxes it would otherwise have to pass on its way in.
 * <br>
 * <br>
 * Packagers place boxes in the order of their search, which is often not a possible insertion order (for example a
 * box placed into a gap under boxes which are already there). The placements are not changed, only their order:
 * a stable topological sort, which keeps the order of the search wherever the rules allow.
 */
public final class InsertionSequencer {

	private InsertionSequencer() {
	}

	/**
	 * Sequence the containers of a result, unless the box items are inserted in a given order.
	 *
	 * @param containers packed containers
	 * @param order the order of the box items; only {@link Order#NONE} (or null) allows reordering
	 * @return true if every container could be sequenced (or no reordering was allowed)
	 */
	public static boolean sequence(List<Container> containers, Order order) {
		if(order != null && order != Order.NONE) {
			return true;
		}
		boolean sequenced = true;
		for (Container container : containers) {
			if(!sequence(container.getStack(), container.getAccess())) {
				sequenced = false;
			}
		}
		return sequenced;
	}

	/**
	 * @param stack the placements of a container
	 * @param access how boxes get into the container
	 * @return true if sequenced; false if the boxes cannot be inserted in any order (the stack is left unchanged)
	 */
	public static boolean sequence(Stack stack, ContainerAccess access) {
		List<Placement> placements = stack.getPlacements();
		int n = placements.size();

		// number of placements which must be inserted before each placement
		int[] predecessors = new int[n];
		for (int i = 0; i < n; i++) {
			Placement first = placements.get(i);
			for (int j = 0; j < n; j++) {
				if(i != j && InsertionOrder.mustPrecede(first, placements.get(j), access)) {
					predecessors[j]++;
				}
			}
		}

		List<Placement> sequence = new ArrayList<>(n);
		boolean[] inserted = new boolean[n];
		// the lowest index which may not be inserted yet
		int start = 0;
		while(sequence.size() < n) {
			// insert the first placement (in the order of the search) which can be inserted
			int next = -1;
			for (int i = start; i < n; i++) {
				if(!inserted[i] && predecessors[i] == 0) {
					next = i;
					break;
				}
			}
			if(next == -1) {
				// cycle: no possible insertion order
				return false;
			}
			inserted[next] = true;
			Placement placement = placements.get(next);
			sequence.add(placement);
			for (int j = 0; j < n; j++) {
				if(!inserted[j] && InsertionOrder.mustPrecede(placement, placements.get(j), access)) {
					predecessors[j]--;
				}
			}
			while(start < n && inserted[start]) {
				start++;
			}
		}

		stack.clear();
		for (int i = 0; i < n; i++) {
			Placement placement = sequence.get(i);
			placement.setIndex(i);
			stack.add(placement);
		}
		return true;
	}
}
