package com.github.skjolber.packing.comparator;

import java.util.Comparator;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;

public class DefaultIntermediatePackagerResultComparator implements Comparator<IntermediatePackagerResult> {

	public static final int ARGUMENT_1_IS_BETTER = 1;
	public static final int ARGUMENT_2_IS_BETTER = -1;
	
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
		if(o1.size() > o2.size()) {
			return ARGUMENT_1_IS_BETTER;
		} else if(o1.size() < o2.size()) {
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
