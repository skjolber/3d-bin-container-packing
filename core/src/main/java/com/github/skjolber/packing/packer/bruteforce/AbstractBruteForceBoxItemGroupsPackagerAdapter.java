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
}
