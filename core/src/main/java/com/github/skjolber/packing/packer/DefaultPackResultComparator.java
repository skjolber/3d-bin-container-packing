package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.packager.PackResult;
import com.github.skjolber.packing.api.packager.PackResultComparator;

public class DefaultPackResultComparator implements PackResultComparator {

	@Override
	public int compare(PackResult o1, PackResult o2) {

		// load volume - more is better
		if(o1.getLoadVolume() > o2.getLoadVolume()) {
			return ARGUMENT_1_IS_BETTER;
		} else if(o1.getLoadVolume() < o2.getLoadVolume()) {
			return ARGUMENT_2_IS_BETTER;
		}

		// load weight - more is better
		if(o1.getLoadWeight() > o2.getLoadWeight()) {
			return ARGUMENT_1_IS_BETTER;
		} else if(o1.getLoadWeight() < o2.getLoadWeight()) {
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
		// total volume - less is better
		if(o1.getVolume() > o2.getVolume()) {
			return ARGUMENT_2_IS_BETTER;
		} else if(o1.getVolume() < o2.getVolume()) {
			return ARGUMENT_1_IS_BETTER;
		}

		// total weight - less is better
		if(o1.getWeight() > o2.getWeight()) {
			return ARGUMENT_2_IS_BETTER;
		} else if(o1.getWeight() < o2.getWeight()) {
			return ARGUMENT_1_IS_BETTER;
		}

		return 0;
	}

}
