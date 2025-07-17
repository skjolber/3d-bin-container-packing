package com.github.skjolber.packing.iterator;

import com.github.skjolber.packing.iterator.DefaultBoxItemGroupPermutationRotationIterator.Builder;

public class DefaultBoxItemGroupPermutationRotationIteratorTest extends AbstractBoxItemGroupsPermutationRotationIteratorTest<DefaultBoxItemGroupPermutationRotationIterator.Builder> {

	@Override
	public Builder newBuilder() {
		return DefaultBoxItemGroupPermutationRotationIterator.newBuilder();
	}

}
