package com.github.skjolber.packing.api.impl;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;

public class PermutationStackableValue {

    protected final int count;
    protected final StackableValue[] values;

    public PermutationStackableValue(int count, Stackable stackable) {
        this.count = count;
        StackValue[] stackValues = stackable.getStackValues();
        values = new StackableValue[stackValues.length];
        for(int i = 0; i < values.length; i++) {
        	values[i] = new StackableValue(stackable, stackValues[i]);
        }
    }

    public StackableValue[] getBoxes() {
        return values;
    }

    public int getCount() {
        return count;
    }
    
}
