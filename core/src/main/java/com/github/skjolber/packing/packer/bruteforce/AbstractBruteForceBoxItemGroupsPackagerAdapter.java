package com.github.skjolber.packing.packer.bruteforce;
import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;

public abstract class AbstractBruteForceBoxItemGroupsPackagerAdapter extends AbstractBruteForceBoxItemPackagerAdapter {

	protected List<BoxItemGroup> boxItemGroups;
	/** The number of groups accepted so far. The iterators keep the groups' initial positions. */
	protected int acceptedGroupCount;

	public AbstractBruteForceBoxItemGroupsPackagerAdapter(List<BoxItem> boxItems,
			ContainerItemsCalculator packagerContainerItems, List<BoxItemGroup> boxItemGroups) {
		super(boxItems, packagerContainerItems);
		
		this.boxItemGroups = boxItemGroups;
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

	/**
	 * @param excluded the groups which the container's iterator excludes, as they do not fit the container
	 * @return whether the container can load the next remaining group (groups are packed in order)
	 */
	protected boolean canLoadNextGroup(List<BoxItemGroup> excluded) {
		return !boxItemGroups.isEmpty() && !isExcluded(excluded, boxItemGroups.get(0));
	}

	private static boolean isExcluded(List<BoxItemGroup> excluded, BoxItemGroup group) {
		for (BoxItemGroup boxItemGroup : excluded) {
			if(boxItemGroup == group) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Truncate a result to whole groups. Groups are packed in order, so a result can hold the remaining groups up to
	 * the first which the container's iterator excludes (because it does not fit).
	 *
	 * @param result result for the container
	 * @param excluded the groups which the container's iterator excludes
	 * @return the result, possibly with fewer boxes
	 */
	protected BruteForceIntermediatePackagerResult truncateToGroup(BruteForceIntermediatePackagerResult result, List<BoxItemGroup> excluded) {
		if(result == null) {
			return null;
		}
		
		// are we at the border between groups?
		int size = result.getSize();

		int wholeGroupBoxCount = 0;
		for(int k = 0; k < boxItemGroups.size(); k++) {
			BoxItemGroup boxItemGroup = boxItemGroups.get(k);
			
			int groupBoxCount = boxItemGroup.getBoxCount();
			if(isExcluded(excluded, boxItemGroup) || size < wholeGroupBoxCount + groupBoxCount) {
				// excluded by the container, or the group was not packed completely
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
}
