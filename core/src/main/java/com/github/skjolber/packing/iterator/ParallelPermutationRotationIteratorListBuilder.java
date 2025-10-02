package com.github.skjolber.packing.iterator;

@Deprecated
public class ParallelPermutationRotationIteratorListBuilder extends AbstractPermutationRotationIteratorBuilder<ParallelPermutationRotationIteratorListBuilder> {

	private int parallelizationCount = -1;

	public ParallelPermutationRotationIteratorListBuilder withParallelizationCount(int parallelizationCount) {
		this.parallelizationCount = parallelizationCount;

		return this;
	}

	public ParallelPermutationRotationIteratorList build() {
		if(parallelizationCount == -1) {
			throw new IllegalStateException();
		}
		if(maxLoadWeight == -1) {
			throw new IllegalStateException();
		}
		if(dx == -1 || dy == -1 || dz == -1) {
			throw new IllegalStateException();
		}

		PermutationBoxItemValue[] matrix = toMatrix();

		return new ParallelPermutationRotationIteratorList(matrix, parallelizationCount);
	}

}
