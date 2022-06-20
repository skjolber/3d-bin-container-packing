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

	public void removePermutations(List<Integer> removed) {
		delegate.removePermutations(removed);
	}

	public int nextRotation() {
		return delegate.nextWorkUnitRotation(index);
	}
	
	public int nextRotation(int maxIndex) {
		return delegate.nextWorkUnitRotation(index, maxIndex);
	}


	public int[] getPermutations() {
		return delegate.getPermutations(index);
	}

	public PermutationRotation get(int permutationIndex) {
		return delegate.get(index, permutationIndex);
	}

	public int nextPermutation() {
		return delegate.nextWorkUnitPermutation(index);
	}

	public int length() {
		return delegate.length();
	}

	public PermutationRotationState getState() {
		return delegate.getState(index);
	}

	@Override
	public long getMinStackableVolume() {
		return delegate.getMinStackableVolume();
	}

	@Override
	public long getMinStackableArea() {
		return delegate.getMinStackableArea();
	}

	@Override
	public int nextPermutation(int maxIndex) {
		return delegate.nextWorkUnitPermutation(index, maxIndex);
	}

	@Override
	public List<PermutationRotation> get(PermutationRotationState state, int length) {
		return delegate.get(state, length);
	}
	
}
