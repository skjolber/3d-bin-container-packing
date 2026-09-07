package com.github.skjolber.packing.packer.bruteforce;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;

/**
 * 
 * Faster comparison of brute force results; do not populate a Stack.
 * 
 */

public class BruteForceIntermediatePackagerResultComparator extends DefaultIntermediatePackagerResultComparator {

	@Override
	public int compare(IntermediatePackagerResult r1, IntermediatePackagerResult r2) {
		
		if(r1 instanceof BruteForceIntermediatePackagerResult && r2 instanceof BruteForceIntermediatePackagerResult) {
			BruteForceIntermediatePackagerResult br1 = (BruteForceIntermediatePackagerResult)r1;
			BruteForceIntermediatePackagerResult br2 = (BruteForceIntermediatePackagerResult)r2;
			
			// load volume - more is better
			if(br1.getLoadVolume() > br2.getLoadVolume()) {
				return ARGUMENT_1_IS_BETTER;
			} else if(br1.getLoadVolume() < br2.getLoadVolume()) {
				return ARGUMENT_2_IS_BETTER;
			}

			// load weight - more is better
			if(br1.getLoadWeight() > br2.getLoadWeight()) {
				return ARGUMENT_1_IS_BETTER;
			} else if(br1.getLoadWeight() < br2.getLoadWeight()) {
				return ARGUMENT_2_IS_BETTER;
			}

			// load count - more is better
			if(br1.getSize() > br2.getSize()) {
				return ARGUMENT_1_IS_BETTER;
			} else if(br1.getSize() < br2.getSize()) {
				return ARGUMENT_2_IS_BETTER;
			}

			// are both empty?
			if(br1.isEmpty()) {
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
		
		return super.compare(r1, r2);
	}

}
