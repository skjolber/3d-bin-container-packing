package com.github.skjolber.packing.api;

import java.util.List;

@FunctionalInterface
public interface StackValueComparator<T> {

	int compare(T point1, StackValue stackValue1, T point2, StackValue stackValue2);
	
}
