package com.github.skjolber.packing.packer.bruteforce;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.packer.ContainerItemsCostCalculator;
import com.github.skjolber.packing.packer.ControlledContainerItem;

public abstract class AbstractBruteForceBoxItemGroupsPackagerAdapter extends AbstractBruteForceBoxItemPackagerAdapter {

	protected List<BoxItemGroup> boxItemGroups;
	protected final List<BoxItemGroup> initialBoxItemGroups;

	public AbstractBruteForceBoxItemGroupsPackagerAdapter(List<BoxItem> boxItems,
			List<ControlledContainerItem> containers, List<BoxItemGroup> boxItemGroups) {
		super(boxItems, containers);
		this.initialBoxItemGroups = copyBoxItemGroups(boxItemGroups);
		
		this.boxItemGroups = boxItemGroups;
	}

	protected AbstractBruteForceBoxItemGroupsPackagerAdapter(AbstractBruteForceBoxItemGroupsPackagerAdapter source) {
		super(source);
		this.initialBoxItemGroups = copyBoxItemGroups(source.initialBoxItemGroups);
		this.boxItemGroups = copyBoxItemGroups(source.boxItemGroups);
	}

	@Override
	public int getMaximumContainerCount(int requestedLimit) {
		return super.getMaximumContainerCount(Math.min(requestedLimit, boxItemGroups.size()));
	}

	@Override
	public long estimateMinimumCost(ContainerItemsCostCalculator calculator, int maxCount) {
		return calculator.getGroupMinimumCost(packagerContainerItems, boxItemGroups, maxCount);
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
