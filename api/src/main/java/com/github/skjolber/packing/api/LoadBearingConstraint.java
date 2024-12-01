package com.github.skjolber.packing.api;

import java.util.List;

import com.github.skjolber.packing.api.packager.BoundedStackable;

/**
 * 
 * Constraint for load (weight) on a {@linkplain StackValue};
 * 
 */

public interface LoadBearingConstraint {

	List<BoundedStackable> filter(List<BoundedStackable> stackables);
	
	boolean canAdd(StackValue value, int x, int y, int z, int weight);

	void add(StackPlacement stackPlacement, int weight);

	void append(StackValue value, int weight);

	void canAppend(StackValue value, int weight);

}
