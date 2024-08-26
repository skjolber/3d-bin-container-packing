package com.github.skjolber.packing.api;

/**
 * 
 * Interface for reporting whether it is okey to add a stackable to a stack, given the addition of another stackable.
 * 
 * This might be used to limit the number of special packages in each container, i.e. only one battery per contaner.
 * 
 */

public interface StackableLoadConstraint {

	boolean canLoad(Stackable stackable, Stackable loaded);

}
