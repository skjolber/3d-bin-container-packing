package com.github.skjolber.packing.packer.bruteforce;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.collections.api.iterator.IntIterator;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.BruteForcePointIteratorFilter;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.DefaultPointFilter;

/**
 * Point filter which counts how often it is used, otherwise returning all fitting points. Thread-safe.
 */
class CountingPointFilter implements BruteForcePointIteratorFilter {

	private final BruteForcePointIteratorFilter delegate = new DefaultPointFilter();
	private final AtomicInteger count = new AtomicInteger();

	@Override
	public IntIterator getPoints(DefaultPointCalculator3D pointCalculator, BoxStackValue stackValue) {
		count.incrementAndGet();
		return delegate.getPoints(pointCalculator, stackValue);
	}

	int getCount() {
		return count.get();
	}
}
