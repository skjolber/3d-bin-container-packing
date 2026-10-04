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
	/** The number of groups accepted so far. The iterators keep the groups' initial positions. */
	protected int acceptedGroupCount;

	public AbstractBruteForceBoxItemGroupSession(List<BoxItem> boxItems,
			List<ContainerItem> containers, int containerCount, List<BoxItemGroup> boxItemGroups) {
		super(boxItems, new BoxItemGroupsContainerItemsCalculator(containers, containerCount, boxItemGroups));
		this.initialBoxItemGroups = copyBoxItemGroups(boxItemGroups);
		
		this.boxItemGroups = boxItemGroups;
	}

	protected AbstractBruteForceBoxItemGroupSession(AbstractBruteForceBoxItemGroupSession source) {
		super(source);
		this.initialBoxItemGroups = copyBoxItemGroups(source.initialBoxItemGroups);
		this.boxItemGroups = copyBoxItemGroups(source.boxItemGroups);
		this.acceptedGroupCount = source.acceptedGroupCount;
	}

	/**
	 * Translate positions in the remaining groups to the iterators' positions, and count the groups as accepted.
	 * Groups are accepted in order, so the remaining groups follow the accepted groups.
	 *
	 * @param groupIndexes positions in the remaining groups
	 * @return positions in the iterators
	 */
	protected List<Integer> acceptGroups(List<Integer> groupIndexes) {
		List<Integer> indexes = new ArrayList<>(groupIndexes.size());
		for(Integer groupIndex : groupIndexes) {
			indexes.add(acceptedGroupCount + groupIndex);
		}
		acceptedGroupCount += groupIndexes.size();
		return indexes;
	}

	@Override
	public List<BoxItemGroup> getRemainingBoxItemGroups() {
		return boxItemGroups;
	}

	@Override
	public int countRemainingBoxItemGroups() {
		return boxItemGroups.size();
	}


	protected BruteForceIntermediatePackagerResult truncateToGroup(BruteForceIntermediatePackagerResult result) {
		if(result == null) {
			return null;
		}
		
		// are we at the border between groups?
		int size = result.getSize();

		// TODO only handles groups in order.
		int wholeGroupBoxCount = 0;
		for(int k = 0; k < boxItemGroups.size(); k++) {
			BoxItemGroup boxItemGroup = boxItemGroups.get(k);
			
			int groupBoxCount = boxItemGroup.getBoxCount();
			if(size < wholeGroupBoxCount + groupBoxCount) {
				// the last group was not successful
				result.trimToSize(wholeGroupBoxCount);
				
				break;
			}
			
			wholeGroupBoxCount += groupBoxCount;
			
			if(wholeGroupBoxCount == size) {
				// do nothing
				break;
			}
		}
		return result;
	}

	/**
	 * Verify that a foreign result consumes complete leading groups and translate
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
		for(int groupIndex = 0; groupIndex < boxItemGroups.size(); groupIndex++) {
			BoxItemGroup group = boxItemGroups.get(groupIndex);
			boolean present = false;
			for(BoxItem item : group.getItems()) {
				Integer count = countByGlobalIndex.remove(item.getGlobalIndex());
				if(count != null) {
					present = true;
				}
				if(count == null || count != item.getCount()) {
					if(!present && countByGlobalIndex.isEmpty()) {
						return new AcceptedGroups(groups, localIndexes);
					}
					throw new IllegalArgumentException("Result does not contain complete box item group " + groupIndex);
				}
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
