package com.github.skjolber.packing.api;

import java.util.List;

public interface StackScope {

	List<StackValue> getStackables();

	void stacked(int index);
}