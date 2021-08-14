package com.github.skjolber.packing.api.impl;

import java.util.List;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;

public class PermutationStackableValue {

    protected final int count;
    protected final PermutationRotation[] values;

    public PermutationStackableValue(int count, Stackable stackable, List<StackValue> boundStackValues) {
        this.count = count;
        this.values = new PermutationRotation[boundStackValues.size()];
        for(int i = 0; i < values.length; i++) {
        	values[i] = new PermutationRotation(stackable, boundStackValues.get(i));
        }
    }

    public PermutationRotation[] getBoxes() {
        return values;
    }

    public int getCount() {
        return count;
    }
    
}
