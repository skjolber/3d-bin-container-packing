package com.github.skjolber.jmh;

import java.util.List;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.impl.PermutationRotationIterator;
import com.github.skjolber.packing.impl.PermutationRotationState;

public class SinglePermutationRotationIterator implements PermutationRotationIterator {

	private final PermutationRotationIterator iterator;
	private final boolean permutate;
	private final boolean rotate;
	public SinglePermutationRotationIterator(PermutationRotationIterator iterator, boolean permutate, boolean rotate) {
		this.iterator = iterator;
		
		this.permutate = permutate;
		this.rotate = rotate;
	}

	public void removePermutations(int count) {
		iterator.removePermutations(count);
	}

	public void removePermutations(List<Integer> removed) {
		iterator.removePermutations(removed);
	}

	public int[] getPermutations() {
		return iterator.getPermutations();
	}

	public int length() {
		return iterator.length();
	}

	public Box get(int index) {
		return iterator.get(index);
	}

	public int nextRotation() {
		if(!rotate) {
			return -1;
		}
		return iterator.nextRotation();
	}

	public int nextPermutation() {
		if(!permutate) {
			return -1;
		}
		return iterator.nextPermutation();
	}

	public PermutationRotationState getState() {
		return iterator.getState();
	}

	public void setState(PermutationRotationState state) {
		iterator.setState(state);
	}
	
}
