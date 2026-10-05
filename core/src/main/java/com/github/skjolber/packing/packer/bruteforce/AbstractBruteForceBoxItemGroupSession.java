package com.github.skjolber.packing.packer.bruteforce;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
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
