package com.github.skjolber.packing.api;

import java.util.List;

public interface FixedOrderStackScopeConstraint {

	List<Stackable> getStackScope();

	void stacked(int index);

}
