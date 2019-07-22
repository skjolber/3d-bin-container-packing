package com.github.skjolber.packing.impl;

import com.github.skjolber.packing.Box;

public interface PermutationRotationIterator extends PermutationSet {

	int[] getPermutations();

	int length();
	
	Box get(int index);

	boolean nextRotation();

	boolean nextPermutation();

	PermutationRotationState getState();

	void setState(PermutationRotationState state);

}