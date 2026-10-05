package com.github.skjolber.packing.packer;

import java.util.Arrays;
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
 * box placed into a gap under boxes which are already there). The placements are not changed, only their order. Without
 * access restrictions, or from the top, the placements are ordered by height (in linear time when already ordered,
 * otherwise n log n); through a door, by a stable topological sort, which keeps the order of the search wherever the
 * rules allow (quadratic in the number of placements).
 */
public final class InsertionSequencer {

	private InsertionSequencer() {
	}

	/**
	 * Sequence the containers of a result, unless the box items are inserted in a given order.
	 *
	 * @param containers packed containers
	 * @param order the order of the box items; only {@link Order#NONE} (or null) allows reordering
	 * @return true if every container is in insertion order: sequenced, or packed with a box item order (when the
	 *         packagers only place boxes which can be inserted); false if some container cannot be sequenced
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
		if(access == ContainerAccess.FRONT) {
			return sequenceTopologically(stack, access);
		}
		return sequenceByHeight(stack);
	}

	/**
	 * Without access restrictions, or from the top, a box must be inserted after the boxes it rests on and (from the top)
	 * the boxes below it, which all start lower: so the placements ordered by their lowest z (keeping the order of the
	 * search for equal z) are in a possible insertion order. Already ordered placements, which are common, are checked
	 * in linear time.
	 */
	protected static boolean sequenceByHeight(Stack stack) {
		List<Placement> placements = stack.getPlacements();
		int n = placements.size();
		boolean ordered = true;
		for (int i = 1; i < n; i++) {
			if(placements.get(i).getAbsoluteZ() < placements.get(i - 1).getAbsoluteZ()) {
				ordered = false;
				break;
			}
		}
		if(ordered) {
			for (int i = 0; i < n; i++) {
				placements.get(i).setIndex(i);
			}
			return true;
		}
		// sort by z, then by the order of the search
		long[] keys = new long[n];
		for (int i = 0; i < n; i++) {
			keys[i] = ((long)placements.get(i).getAbsoluteZ() << 32) | i;
		}
		Arrays.sort(keys);
		Placement[] search = placements.toArray(new Placement[n]);
		stack.clear();
		for (int i = 0; i < n; i++) {
			Placement placement = search[(int)keys[i]];
			placement.setIndex(i);
			stack.add(placement);
		}
		return true;
	}

	/**
	 * A stable topological sort: insert the first placement (in the order of the search) which can be inserted, until
	 * all are inserted.
	 */
	protected static boolean sequenceTopologically(Stack stack, ContainerAccess access) {
		List<Placement> placements = stack.getPlacements();
		int n = placements.size();

		// the placements which must be inserted after each placement, and the number which must be inserted before
		int[][] successors = new int[n][];
		int[] successorCounts = new int[n];
		int[] predecessors = new int[n];
		for (int i = 0; i < n; i++) {
			Placement first = placements.get(i);
			for (int j = 0; j < n; j++) {
				if(i != j && InsertionOrder.mustPrecede(first, placements.get(j), access)) {
					int[] list = successors[i];
					if(list == null) {
						list = new int[4];
						successors[i] = list;
					} else if(successorCounts[i] == list.length) {
						list = Arrays.copyOf(list, list.length * 2);
						successors[i] = list;
					}
					list[successorCounts[i]++] = j;
					predecessors[j]++;
				}
			}
		}

		Placement[] sequence = new Placement[n];
		boolean[] inserted = new boolean[n];
		// the lowest index which is not inserted yet
		int start = 0;
		for (int count = 0; count < n; count++) {
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
			sequence[count] = placements.get(next);
			int[] list = successors[next];
			for (int k = 0; k < successorCounts[next]; k++) {
				predecessors[list[k]]--;
			}
			while(start < n && inserted[start]) {
				start++;
			}
		}

		stack.clear();
		for (int i = 0; i < n; i++) {
			sequence[i].setIndex(i);
			stack.add(sequence[i]);
		}
		return true;
	}
}
