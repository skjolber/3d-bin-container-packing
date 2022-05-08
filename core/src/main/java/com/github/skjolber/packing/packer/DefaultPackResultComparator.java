package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.PackResult;
import com.github.skjolber.packing.api.PackResultComparator;
import com.github.skjolber.packing.packer.plain.PlainPackagerResult;

public class DefaultPackResultComparator implements PackResultComparator {

	
	/**
	 * 
	 * Returns a negative integer if o1 is less / worse than o2. 
	 * Returns a positive integer if o1 is more / better than o2. 
	 * 
	 * Return 0 otherwise.
	 */
	
	@Override
	public int compare(PackResult o1, PackResult o2) {
		
		PlainPackagerResult plainResult = (PlainPackagerResult)result;
		if(stack.getSize() >= plainResult.stack.getSize()) {
			return true;
		} else if(stack.getSize() == plainResult.stack.getSize()) {
			return container.getWeight() >= plainResult.container.getWeight();
		}
		
		return false;
		
		
		
		return 0;
	}

}
