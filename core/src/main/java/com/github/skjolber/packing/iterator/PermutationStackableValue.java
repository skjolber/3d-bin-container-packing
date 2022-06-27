package com.github.skjolber.packing.iterator;

import java.util.List;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;

public class PermutationStackableValue {

    protected final int index;
    protected final int count;
    protected final PermutationRotation[] values;
    protected final Stackable stackable;

	protected final long minVolumeLimit;
	protected final long minAreaLimit;
    
    public PermutationStackableValue(int index, int count, Stackable stackable, List<StackValue> stackValues) {
    	this.index = index;
        this.count = count;
        this.values = new PermutationRotation[stackValues.size()];
        this.stackable = stackable;
        
    	long minVolumeLimit = Long.MAX_VALUE;
    	long minAreaLimit = Long.MAX_VALUE;
        
        for(int i = 0; i < values.length; i++) {
        	StackValue stackValue = stackValues.get(i);
        	
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
    
    public int getIndex() {
		return index;
	}
    
    public Stackable getStackable() {
		return stackable;
	}
}
