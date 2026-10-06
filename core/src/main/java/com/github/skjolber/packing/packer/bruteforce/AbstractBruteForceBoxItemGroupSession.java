package com.github.skjolber.packing.packer.bruteforce;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.packer.BoxItemGroupsContainerItemsCalculator;

public abstract class AbstractBruteForceBoxItemGroupSession extends AbstractBruteForceBoxItemSession {

	protected record AcceptedGroups(List<Integer> groupIndexes, List<Integer> localIndexes) {
	}

	protected List<BoxItemGroup> boxItemGroups;
	protected final List<BoxItemGroup> initialBoxItemGroups;
	/**
	 * The iterators' positions of the remaining groups, in order. The iterators keep the groups' initial positions:
	 * results from this session hold the first remaining groups, results from other packagers any complete groups.
	 */
	protected List<Integer> remainingGroupPositions;

	public AbstractBruteForceBoxItemGroupSession(List<BoxItem> boxItems,
			List<ContainerItem> containers, int containerCount, List<BoxItemGroup> boxItemGroups) {
		super(boxItems, new BoxItemGroupsContainerItemsCalculator(containers, containerCount, boxItemGroups));
		this.initialBoxItemGroups = copyBoxItemGroups(boxItemGroups);
		
		this.boxItemGroups = boxItemGroups;
		this.remainingGroupPositions = new ArrayList<>(boxItemGroups.size());
		for(int i = 0; i < boxItemGroups.size(); i++) {
			remainingGroupPositions.add(i);
		}
	}

	protected AbstractBruteForceBoxItemGroupSession(AbstractBruteForceBoxItemGroupSession source) {
		super(source);
		this.initialBoxItemGroups = copyBoxItemGroups(source.initialBoxItemGroups);
		this.boxItemGroups = copyBoxItemGroups(source.boxItemGroups);
		this.remainingGroupPositions = new ArrayList<>(source.remainingGroupPositions);
		this.groupOrderSearch = source.groupOrderSearch;
		this.skipGroupOrders = source.skipGroupOrders;
	}

	/** Search the orders of the remaining groups when there are at most this many (by default all; 0 for never) */
	protected int groupOrderSearch = Integer.MAX_VALUE;

	/** Skip the orders which cannot give a better result (see {@link #attemptGroupOrders}); tests turn this off */
	boolean skipGroupOrders = true;

	public void setGroupOrderSearch(int groupOrderSearch) {
		this.groupOrderSearch = groupOrderSearch;
	}

	@Override
	public PackagerSession fresh() {
		PackagerSession fresh = super.fresh();
		((AbstractBruteForceBoxItemGroupSession)fresh).setGroupOrderSearch(groupOrderSearch);
		((AbstractBruteForceBoxItemGroupSession)fresh).skipGroupOrders = skipGroupOrders;
		return fresh;
	}

	/**
	 * @return whether to search the orders of the remaining groups in this attempt (see {@link #attemptGroupOrders})
	 */
	protected boolean isGroupOrderSearch() {
		return order == Order.NONE && boxItemGroups.size() > 1 && boxItemGroups.size() <= groupOrderSearch;
	}

	/**
	 * Pack the remaining groups in each of their orders, and keep the best result. Groups are packed in order, and a
	 * container holds the first groups which fit: another order can fill the container better, for example by
	 * leaving out a group which does not fit with the others.
	 * <br>
	 * <br>
	 * A result holds the groups before the first group which it does not hold completely (or which does not fit the
	 * container at all). The orders which begin with the same groups up to and including that group cannot give a
	 * better result, so they are skipped (see {@link #nextGroupOrder(int[], int)}). When a result holds all groups, no
	 * other order is better.
	 * <br>
	 * <br>
	 * With container priorities, the groups of a priority come after the groups of the lower priorities in each order:
	 * the remaining groups are sorted by priority, and only the groups of the same priority change places.
	 *
	 * @param containerIndex the container
	 * @param best the best result so far, or null
	 * @return the best result, or null if no group fits the container
	 */
	protected BruteForceIntermediatePackagerResult attemptGroupOrders(int containerIndex, IntermediatePackagerResult best) throws PackagerInterruptedException {
		Container container = packagerContainerItems.getContainerItem(containerIndex).getContainer();

		int[] groupOrder = new int[boxItemGroups.size()];
		for (int k = 0; k < groupOrder.length; k++) {
			groupOrder[k] = k;
		}
		BruteForceIntermediatePackagerResult bestResult = null;
		// the position of the first group which the last result did not hold completely
		int decisive = 0;
		do {
			int violation = getContainerPriorityViolation(groupOrder);
			if(violation != -1) {
				// neither does any order which begins with the same groups
				decisive = violation;
				continue;
			}
			List<BoxItemGroup> groups = copyGroups(boxItemGroups, groupOrder);
			DefaultBoxItemGroupPermutationRotationIterator iterator = DefaultBoxItemGroupPermutationRotationIterator.newBuilder()
					.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
					.withBoxItemGroups(groups)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.build();
			BoxItemGroup[] iteratorGroups = iterator.getBoxItemGroups();
			if(iteratorGroups[0] == null) {
				// the first group does not fit the container: neither does any order which begins with it
				decisive = 0;
				continue;
			}
			BruteForceIntermediatePackagerResult result = truncateToWholeGroups(packGroupOrder(containerIndex, iterator, groupOrder, best), iteratorGroups);
			int size = result != null ? result.getSize() : 0;
			if(size > 0 && (bestResult == null || getIntermediatePackagerResultComparator().compare(bestResult, result) < 0)) {
				result.setAnyRemaining(true);
				bestResult = result;
			}
			if(!skipGroupOrders) {
				decisive = groupOrder.length - 1;
				continue;
			}
			decisive = getFirstIncompleteGroup(iteratorGroups, size);
			if(decisive == iteratorGroups.length) {
				// all groups fit: no order is better
				break;
			}
		} while(nextGroupOrder(groupOrder, decisive));
		return bestResult;
	}

	/**
	 * @return the first position in the order where a group has a lower container priority than the group before it, or
	 *         -1 if none
	 */
	private int getContainerPriorityViolation(int[] groupOrder) {
		for (int k = 1; k < groupOrder.length; k++) {
			if(boxItemGroups.get(groupOrder[k]).getContainerPriority() < boxItemGroups.get(groupOrder[k - 1]).getContainerPriority()) {
				return k;
			}
		}
		return -1;
	}

	/**
	 * @param iteratorGroups an iterator's groups in their order (null if excluded)
	 * @param size the number of boxes of a result holding whole groups
	 * @return the position of the first group which the result does not hold completely, or the number of groups if it
	 *         holds all of them
	 */
	protected static int getFirstIncompleteGroup(BoxItemGroup[] iteratorGroups, int size) {
		int count = 0;
		for (int p = 0; p < iteratorGroups.length; p++) {
			BoxItemGroup group = iteratorGroups[p];
			if(group == null || size < count + group.getBoxCount()) {
				return p;
			}
			count += group.getBoxCount();
		}
		return iteratorGroups.length;
	}

	/**
	 * Pack the boxes of an iterator into a container, as {@link #attempt(int, IntermediatePackagerResult, boolean)}
	 * does with the session's iterators.
	 *
	 * @param iterator the iterator for the groups in the order
	 * @param groupOrder the order of the remaining groups (see {@link #copyGroups(List, int[])})
	 */
	protected abstract BruteForceIntermediatePackagerResult packGroupOrder(int containerIndex, BoxItemPermutationRotationIterator iterator, int[] groupOrder, IntermediatePackagerResult best) throws PackagerInterruptedException;

	protected abstract Comparator<IntermediatePackagerResult> getIntermediatePackagerResultComparator();

	/**
	 * Copies of the groups in an order, for an iterator of their own: building an iterator points the boxes' stack
	 * values to the iterator's boxes, so iterators must not share them (results are calculated from them later).
	 */
	protected static List<BoxItemGroup> copyGroups(List<BoxItemGroup> groups, int[] groupOrder) {
		List<BoxItemGroup> copies = new ArrayList<>(groupOrder.length);
		for (int k : groupOrder) {
			BoxItemGroup group = groups.get(k);
			List<BoxItem> items = new ArrayList<>(group.size());
			for (BoxItem item : group.getItems()) {
				Box box = item.getBox();
				BoxStackValue[] stackValues = box.getStackValues();
				List<BoxStackValue> stackValueCopies = new ArrayList<>(stackValues.length);
				for (BoxStackValue stackValue : stackValues) {
					stackValueCopies.add(stackValue.copy());
				}
				items.add(new BoxItem(new Box(box, stackValueCopies), item.getCount(), item.getLocalIndex(), item.getGlobalIndex()).withOrderingOf(item));
			}
			copies.add(new BoxItemGroup(group.getId(), items));
		}
		return copies;
	}

	/**
	 * Skip to the next order which differs from this order up to and including a position.
	 *
	 * @param position the position
	 * @return true if there is such an order (lexicographically), false if this was the last
	 */
	protected boolean nextGroupOrder(int[] groupOrder, int position) {
		// the last order with these groups up to the position: the other groups in descending order
		for (int a = position + 1; a < groupOrder.length; a++) {
			for (int b = a + 1; b < groupOrder.length; b++) {
				if(groupOrder[b] > groupOrder[a]) {
					int swap = groupOrder[a];
					groupOrder[a] = groupOrder[b];
					groupOrder[b] = swap;
				}
			}
		}
		return nextGroupOrder(groupOrder);
	}

	/**
	 * @return true if there is a next order (lexicographically), false if this was the last
	 */
	protected static boolean nextGroupOrder(int[] groupOrder) {
		int i = groupOrder.length - 2;
		while(i >= 0 && groupOrder[i] >= groupOrder[i + 1]) {
			i--;
		}
		if(i < 0) {
			return false;
		}
		int j = groupOrder.length - 1;
		while(groupOrder[j] <= groupOrder[i]) {
			j--;
		}
		int swap = groupOrder[i];
		groupOrder[i] = groupOrder[j];
		groupOrder[j] = swap;
		for (int a = i + 1, b = groupOrder.length - 1; a < b; a++, b--) {
			swap = groupOrder[a];
			groupOrder[a] = groupOrder[b];
			groupOrder[b] = swap;
		}
		return true;
	}

	/**
	 * With {@link Order#CHRONOLOGICAL_ALLOW_SKIPPING}, groups are skipped whole.
	 *
	 * @param iteratorGroups the iterator's groups in their order (null if excluded)
	 * @param length the iterator's number of boxes
	 * @return for each box of the iterator's permutation, the box to continue with when skipping it: the first box of
	 *         the next group for the first box of a group, otherwise -1
	 */
	protected static int[] getGroupSkipEnds(BoxItemGroup[] iteratorGroups, int length) {
		int[] skipEnds = new int[length];
		int start = 0;
		for (BoxItemGroup group : iteratorGroups) {
			if(group == null) {
				continue;
			}
			int count = group.getBoxCount();
			skipEnds[start] = start + count;
			for (int k = start + 1; k < start + count; k++) {
				skipEnds[k] = -1;
			}
			start += count;
		}
		return skipEnds;
	}

	/**
	 * Truncate a result to whole groups, for an iterator of an order of the remaining groups.
	 *
	 * @param iteratorGroups the iterator's groups in their order (null if excluded)
	 */
	protected static BruteForceIntermediatePackagerResult truncateToWholeGroups(BruteForceIntermediatePackagerResult result, BoxItemGroup[] iteratorGroups) {
		if(result == null) {
			return null;
		}
		int size = result.getSize();
		int wholeGroupBoxCount = 0;
		for (BoxItemGroup group : iteratorGroups) {
			if(group == null || size < wholeGroupBoxCount + group.getBoxCount()) {
				// excluded by the container, or the group was not packed completely
				result.trimToSize(wholeGroupBoxCount);
				break;
			}
			wholeGroupBoxCount += group.getBoxCount();
			if(wholeGroupBoxCount == size) {
				break;
			}
		}
		return result;
	}

	/**
	 * Accept groups: remove them from the remaining groups.
	 *
	 * @param groupIndexes positions in the remaining groups, ascending
	 * @return positions in the iterators
	 */
	protected List<Integer> acceptGroups(List<Integer> groupIndexes) {
		List<Integer> positions = new ArrayList<>(groupIndexes.size());
		for(Integer groupIndex : groupIndexes) {
			positions.add(remainingGroupPositions.get(groupIndex));
		}
		List<BoxItemGroup> remaining = new ArrayList<>(boxItemGroups);
		for(int i = groupIndexes.size() - 1; i >= 0; i--) {
			int groupIndex = groupIndexes.get(i);
			remaining.remove(groupIndex);
			remainingGroupPositions.remove(groupIndex);
		}
		boxItemGroups = remaining;
		return positions;
	}

	@Override
	public List<BoxItemGroup> getRemainingBoxItemGroups() {
		return boxItemGroups;
	}

	@Override
	public int countRemainingBoxItemGroups() {
		return boxItemGroups.size();
	}


	/**
	 * @param iteratorGroups a container iterator's groups, by initial position (null if excluded or accepted)
	 * @return whether the container can load the next remaining group (groups are packed in order)
	 */
	protected boolean canLoadNextGroup(BoxItemGroup[] iteratorGroups) {
		return !boxItemGroups.isEmpty() && iteratorGroups[remainingGroupPositions.get(0)] != null;
	}

	/**
	 * Truncate a result to whole groups. Groups are packed in order, so a result can hold the remaining groups up to
	 * the first which the container's iterator excludes (because it does not fit).
	 *
	 * @param result result for the container
	 * @param iteratorGroups the container iterator's groups, by initial position (null if excluded or accepted)
	 * @return the result, possibly with fewer boxes
	 */
	protected BruteForceIntermediatePackagerResult truncateToGroup(BruteForceIntermediatePackagerResult result, BoxItemGroup[] iteratorGroups) {
		if(result == null) {
			return null;
		}

		// are we at the border between groups?
		int size = result.getSize();

		int wholeGroupBoxCount = 0;
		for(int k = 0; k < boxItemGroups.size(); k++) {
			int groupBoxCount = boxItemGroups.get(k).getBoxCount();
			if(iteratorGroups[remainingGroupPositions.get(k)] == null || size < wholeGroupBoxCount + groupBoxCount) {
				// excluded by the container, or the group was not packed completely
				result.trimToSize(wholeGroupBoxCount);
				break;
			}
			wholeGroupBoxCount += groupBoxCount;
			if(wholeGroupBoxCount == size) {
				break;
			}
		}
		return result;
	}

	/**
	 * Verify that a foreign result consumes complete groups (any of the remaining groups) and translate
	 * its stable box-item identities to this session's local iterator indexes.
	 */
	protected AcceptedGroups getAcceptedGroups(Stack stack) {
		Map<Integer, Integer> countByGlobalIndex = new HashMap<>(stack.size() * 2);
		for(Placement placement : stack.getPlacements()) {
			BoxItem source = (BoxItem) placement.getStackValue().getBox().getBoxItem();
			int globalIndex = source.getGlobalIndex();
			getLocalIndex(globalIndex); // validates that this session owns the item
			countByGlobalIndex.merge(globalIndex, 1, Integer::sum);
		}

		List<Integer> groups = new ArrayList<>();
		List<Integer> localIndexes = new ArrayList<>(stack.size());
		for(int groupIndex = 0; groupIndex < boxItemGroups.size() && !countByGlobalIndex.isEmpty(); groupIndex++) {
			BoxItemGroup group = boxItemGroups.get(groupIndex);
			boolean present = false;
			boolean complete = true;
			for(BoxItem item : group.getItems()) {
				Integer count = countByGlobalIndex.get(item.getGlobalIndex());
				if(count != null) {
					present = true;
				}
				if(count == null || count != item.getCount()) {
					complete = false;
				}
			}
			if(!present) {
				continue;
			}
			if(!complete) {
				throw new IllegalArgumentException("Result does not contain complete box item group " + groupIndex);
			}
			for(BoxItem item : group.getItems()) {
				int count = countByGlobalIndex.remove(item.getGlobalIndex());
				int localIndex = getLocalIndex(item.getGlobalIndex());
				for(int i = 0; i < count; i++) {
					localIndexes.add(localIndex);
				}
			}
			groups.add(groupIndex);
		}
		if(!countByGlobalIndex.isEmpty()) {
			throw new IllegalArgumentException("Result contains box items outside the remaining box item groups");
		}
		return new AcceptedGroups(groups, localIndexes);
	}
}
