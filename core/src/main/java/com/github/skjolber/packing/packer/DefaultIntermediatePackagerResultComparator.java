package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.PackResult;
import com.github.skjolber.packing.api.packager.PackResultComparator;

public class DefaultIntermediatePackagerResultComparator implements IntermediatePackagerResultComparator {

	@Override
	public int compare(IntermediatePackagerResult r1, IntermediatePackagerResult r2) {

		Stack o1 = r1.getStack();
		Stack o2 = r2.getStack();
		
		// load volume - more is better
		if(o1.getVolume() > o2.getVolume()) {
			return ARGUMENT_1_IS_BETTER;
		} else if(o1.getVolume() < o2.getVolume()) {
			return ARGUMENT_2_IS_BETTER;
		}

		// load weight - more is better
		if(o1.getWeight() > o2.getWeight()) {
			return ARGUMENT_1_IS_BETTER;
		} else if(o1.getWeight() < o2.getWeight()) {
			return ARGUMENT_2_IS_BETTER;
		}

		// load count - more is better
		if(o1.getSize() > o2.getSize()) {
			return ARGUMENT_1_IS_BETTER;
		} else if(o1.getSize() < o2.getSize()) {
			return ARGUMENT_2_IS_BETTER;
		}

		// are both empty?
		if(o1.isEmpty()) {
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
