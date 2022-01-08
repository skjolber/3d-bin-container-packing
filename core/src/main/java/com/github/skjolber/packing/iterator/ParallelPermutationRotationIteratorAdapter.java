package com.github.skjolber.packing.iterator;

import java.util.List;

public class ParallelPermutationRotationIteratorAdapter implements PermutationRotationIterator {

	private final ParallelPermutationRotationIterator delegate;
	private final int index;

	// try to avoid false sharing by using padding
	public long t0, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15 = -1L;
	
	public long preventOptmisation(){
		return t0 + t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8 + t9 + t10 + t11 + t12 + t13 + t14 + t15;
	}
	
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

	public int nextRotation() {
		return delegate.nextRotation(index);
	}

	public int[] getPermutations() {
		return delegate.getPermutations(index);
	}

	public PermutationRotation get(int index) {
		return delegate.get(index, this.index);
	}

	public int nextPermutation() {
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
