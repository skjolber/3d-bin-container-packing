package com.github.skjolberg.packing.impl;

import com.github.skjolberg.packing.Box;

public interface PermutationRotationIterator extends PermutationSet {

	int[] getPermutations();

	int length();
	
	Box get(int index);

	boolean nextRotation();

	boolean nextPermutation();

	PermutationRotationState getState();

	void setState(PermutationRotationState state);

}