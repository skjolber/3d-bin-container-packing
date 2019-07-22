package com.github.skjolber.packing.impl;

import java.util.List;

import com.github.skjolber.packing.Box;

public class ParallelPermutationRotationIteratorAdapter implements PermutationRotationIterator {

	private final ParallelPermutationRotationIterator delegate;
	private final int index;

	public ParallelPermutationRotationIteratorAdapter(ParallelPermutationRotationIterator delegate, int index) {
		super();
		this.delegate = delegate;
		this.index = index;
	}

	public void removePermutations(int count) {
		delegate.removePermutations(count);
	}

	public void removePermutations(List<Integer> removed) {
		delegate.removePermutations(removed);
	}

	public boolean nextRotation() {
		return delegate.nextRotation(index);
	}

	public int[] getPermutations() {
		return delegate.getPermutations(index);
	}

	public Box get(int index) {
		return delegate.get(index, this.index);
	}

	public boolean nextPermutation() {
		return delegate.nextPermutation(index);
	}

	public int length() {
		return delegate.length();
	}

	public PermutationRotationState getState() {
		return delegate.getState(index);
	}

	public void setState(PermutationRotationState state) {
		delegate.setState(state);
	}
	
}
