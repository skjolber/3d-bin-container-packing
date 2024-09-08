package com.github.skjolber.packing.iterator;

import com.github.skjolber.packing.iterator.DefaultLoadableItemPermutationRotationIterator.Builder;

class LoadableItemPermutationRotationIteratorTest extends AbstractLoadablePermutationRotationIteratorTest<DefaultLoadableItemPermutationRotationIterator.Builder> {

	@Override
	public Builder newBuilder() {
		return DefaultLoadableItemPermutationRotationIterator.newBuilder();
	}

}
