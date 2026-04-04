package com.github.skjolber.packing.packer.bruteforce;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.packer.ContainerItemLoadsCalculator;

public abstract class AbstractBruteForceBoxItemGroupsPackagerAdapter extends AbstractBruteForceBoxItemPackagerAdapter {

	protected List<BoxItemGroup> boxItemGroups;

	public AbstractBruteForceBoxItemGroupsPackagerAdapter(List<BoxItem> boxItems,
			ContainerItemLoadsCalculator packagerContainerItems, List<BoxItemGroup> boxItemGroups) {
		super(boxItems, packagerContainerItems);
		
		this.boxItemGroups = boxItemGroups;
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
