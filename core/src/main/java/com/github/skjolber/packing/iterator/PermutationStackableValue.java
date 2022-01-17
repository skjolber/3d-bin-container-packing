package com.github.skjolber.packing.iterator;

import java.util.List;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;

public class PermutationStackableValue {

    protected final int count;
    protected final PermutationRotation[] values;

	protected final long minVolumeLimit;
	protected final long minAreaLimit;
    
    public PermutationStackableValue(int count, Stackable stackable, List<StackValue> boundStackValues) {
        this.count = count;
        this.values = new PermutationRotation[boundStackValues.size()];
        
    	long minVolumeLimit = Long.MAX_VALUE;
    	long minAreaLimit = Long.MAX_VALUE;
        
        for(int i = 0; i < values.length; i++) {
        	StackValue stackValue = boundStackValues.get(i);
        	
        	values[i] = new PermutationRotation(stackable, stackValue);
        	
        	if(minVolumeLimit > stackValue.getVolume()) {
        		minVolumeLimit = stackValue.getVolume();
        	}

        	if(minAreaLimit > stackValue.getArea()) {
        		minAreaLimit = stackValue.getArea();
        	}
        }
        
        this.minAreaLimit = minAreaLimit;
        this.minVolumeLimit = minVolumeLimit;
    }

    public PermutationRotation[] getBoxes() {
        return values;
    }

    public int getCount() {
        return count;
    }

    public long getMinAreaLimit() {
		return minAreaLimit;
	}
    
    public long getMinVolumeLimit() {
		return minVolumeLimit;
	}
}
