package com.github.skjolber.packing.comparator;


import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;

public class DefaultIntermediatePackagerResultComparator implements IntermediatePackagerResultComparator {

	public static final int ARGUMENT_1_IS_BETTER = 1;
	public static final int ARGUMENT_2_IS_BETTER = -1;
	
	@Override
	public boolean prefersHigherLoadVolume() {
		return true;
	}

	@Override
	public int compare(IntermediatePackagerResult r1, IntermediatePackagerResult r2) {

		// not the stacks: brute-force results do not build them (see IntermediatePackagerResultComparator)

		// load volume - more is better
		// (a stack's volume and weight are sums over all placements: read each once)
		long volume1 = r1.getLoadVolume();
		long volume2 = r2.getLoadVolume();
		if(volume1 > volume2) {
			return ARGUMENT_1_IS_BETTER;
		} else if(volume1 < volume2) {
			return ARGUMENT_2_IS_BETTER;
		}

		// load weight - more is better
		long weight1 = r1.getLoadWeight();
		long weight2 = r2.getLoadWeight();
		if(weight1 > weight2) {
			return ARGUMENT_1_IS_BETTER;
		} else if(weight1 < weight2) {
			return ARGUMENT_2_IS_BETTER;
		}

		// load count - more is better
		int count1 = r1.getBoxCount();
		int count2 = r2.getBoxCount();
		if(count1 > count2) {
			return ARGUMENT_1_IS_BETTER;
		} else if(count1 < count2) {
			return ARGUMENT_2_IS_BETTER;
		}

		// are both empty?
		if(count1 == 0) {
			return 0;
		}
		
		Container c1 = r1.getContainerItem().getContainer();
		Container c2 = r2.getContainerItem().getContainer();
		
		// container total volume - less is better
		if(c1.getVolume() > c2.getVolume()) {
			return ARGUMENT_2_IS_BETTER;
		} else if(c1.getVolume() < c2.getVolume()) {
			return ARGUMENT_1_IS_BETTER;
		}

		// empty weight - less is better
		if(c1.getEmptyWeight() > c2.getEmptyWeight()) {
			return ARGUMENT_2_IS_BETTER;
		} else if(c1.getEmptyWeight() < c2.getEmptyWeight()) {
			return ARGUMENT_1_IS_BETTER;
		}

		return 0;
	}

}
