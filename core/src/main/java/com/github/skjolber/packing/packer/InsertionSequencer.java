package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;

/**
 * Reorders the placements of a packed container into a possible insertion order (see {@link ContainerAccess}):
 * each box after the boxes it rests on, and after the boxes it would otherwise have to pass on its way in.
 * <br>
 * <br>
 * Packagers place boxes in the order of their search, which is often not a possible insertion order (for example a
 * box placed into a gap under boxes which are already there). The placements are not changed, only their order. Without
 * access restrictions, or from the top, the placements are ordered by height (in linear time when already ordered,
 * otherwise n log n); through a door, by a stable topological sort, which keeps the order of the search wherever the
 * rules allow (quadratic in the number of placements). Boxes with different extraction orders (see
 * {@code BoxItem.withExtractionOrder(int)}) are inserted in descending extraction order, so that the boxes extracted
 * first are inserted last.
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
		List<Placement> placements = stack.getPlacements();
		Placement[] sequence = new Placement[placements.size()];
		boolean sequenced;
		if(hasExtractionOrders(placements)) {
			sequenced = sequenceByExtractionOrder(placements, access, sequence);
		} else {
			sequenced = sequence(placements, access, sequence, 0);
		}
		if(!sequenced) {
			return false;
		}
		stack.clear();
		for (int i = 0; i < sequence.length; i++) {
			sequence[i].setIndex(i);
			stack.add(sequence[i]);
		}
		return true;
	}

	private static boolean hasExtractionOrders(List<Placement> placements) {
		for (int i = 1; i < placements.size(); i++) {
			if(getExtractionOrder(placements.get(i)) != getExtractionOrder(placements.get(0))) {
				return true;
			}
		}
		return false;
	}

	private static int getExtractionOrder(Placement placement) {
		BoxItem boxItem = placement.getBoxItem();
		return boxItem != null ? boxItem.getExtractionOrder() : 0;
	}

	/**
	 * The boxes which are extracted last are inserted first: sequence the boxes of each extraction order, in descending
	 * order. The packagers place boxes so that none of them must be inserted before a box which is extracted later,
	 * which is checked.
	 *
	 * @param sequence the sequence to fill
	 * @return true if sequenced, false if there is no sequence
	 */
	protected static boolean sequenceByExtractionOrder(List<Placement> placements, ContainerAccess access, Placement[] sequence) {
		int n = placements.size();
		int[] orders = new int[n];
		for (int i = 0; i < n; i++) {
			orders[i] = getExtractionOrder(placements.get(i));
		}
		Arrays.sort(orders);

		int count = 0;
		List<Placement> part = new ArrayList<>();
		for (int k = n - 1; k >= 0; k--) {
			if(k < n - 1 && orders[k] == orders[k + 1]) {
				continue;
			}
			int order = orders[k];
			part.clear();
			for (int i = 0; i < n; i++) {
				Placement placement = placements.get(i);
				if(getExtractionOrder(placement) == order) {
					part.add(placement);
				}
			}
			if(!sequence(part, access, sequence, count)) {
				return false;
			}
			count += part.size();
		}

		// a box extracted earlier is inserted later: it must not have to be inserted first
		for (int i = 0; i < n; i++) {
			Placement placement = sequence[i];
			int order = getExtractionOrder(placement);
			for (int j = 0; j < i; j++) {
				if(getExtractionOrder(sequence[j]) != order && placement.mustPrecede(sequence[j], access)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * @param sequence the sequence to fill
	 * @param offset the index in the sequence of the first placement
	 * @return true if sequenced, false if there is no sequence
	 */
	protected static boolean sequence(List<Placement> placements, ContainerAccess access, Placement[] sequence, int offset) {
		if(access == ContainerAccess.FRONT) {
			return sequenceTopologically(placements, access, sequence, offset);
		}
		sequenceByHeight(placements, sequence, offset);
		return true;
	}

	/**
	 * Without access restrictions, or from the top, a box must be inserted after the boxes it rests on and (from the top)
	 * the boxes below it, which all start lower: so the placements ordered by their lowest z (keeping the order of the
	 * search for equal z) are in a possible insertion order. Already ordered placements, which are common, are checked
	 * in linear time.
	 *
	 * @param sequence the sequence to fill
	 * @param offset the index in the sequence of the first placement
	 */
	protected static void sequenceByHeight(List<Placement> placements, Placement[] sequence, int offset) {
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
				sequence[offset + i] = placements.get(i);
			}
			return;
		}
		// sort by z, then by the order of the search
		long[] keys = new long[n];
		for (int i = 0; i < n; i++) {
			keys[i] = ((long)placements.get(i).getAbsoluteZ() << 32) | i;
		}
		Arrays.sort(keys);
		for (int i = 0; i < n; i++) {
			sequence[offset + i] = placements.get((int)keys[i]);
		}
	}

	/**
	 * A stable topological sort: insert the first placement (in the order of the search) which can be inserted, until
	 * all are inserted.
	 *
	 * @param sequence the sequence to fill
	 * @param offset the index in the sequence of the first placement
	 * @return true if sequenced, false if there is no sequence (a cycle)
	 */
	protected static boolean sequenceTopologically(List<Placement> placements, ContainerAccess access, Placement[] sequence, int offset) {
		int n = placements.size();

		// the placements which must be inserted after each placement, and the number which must be inserted before
		int[][] successors = new int[n][];
		int[] successorCounts = new int[n];
		int[] predecessors = new int[n];
		for (int i = 0; i < n; i++) {
			Placement first = placements.get(i);
			for (int j = 0; j < n; j++) {
				if(i != j && first.mustPrecede(placements.get(j), access)) {
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
			sequence[offset + count] = placements.get(next);
			int[] list = successors[next];
			for (int k = 0; k < successorCounts[next]; k++) {
				predecessors[list[k]]--;
			}
			while(start < n && inserted[start]) {
				start++;
			}
		}
		return true;
	}
}
