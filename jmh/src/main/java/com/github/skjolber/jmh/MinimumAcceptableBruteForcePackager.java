package com.github.skjolber.jmh;

import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.BruteForcePackager;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.Placement;
import com.github.skjolber.packing.impl.BruteForceResult;

public class MinimumAcceptableBruteForcePackager extends BruteForcePackager {

	private final int mimimumAcceptableCount;
	private final boolean permutate;
	private final boolean rotate;
	
	public MinimumAcceptableBruteForcePackager(List<Container> containers, int mimimumAcceptableCount, boolean permutate, boolean rotate) {
		super(containers);
		
		this.mimimumAcceptableCount = mimimumAcceptableCount;
		this.permutate = permutate;
		this.rotate = rotate;
	}

	public MinimumAcceptableBruteForcePackager(List<Container> containers, boolean rotate3d, boolean binarySearch, int checkpointsPerDeadlineCheck, int mimimumAcceptableCount, boolean permutate, boolean rotate) {
		super(containers, rotate3d, binarySearch, checkpointsPerDeadlineCheck);
		
		this.mimimumAcceptableCount = mimimumAcceptableCount;
		this.permutate = permutate;
		this.rotate = rotate;
	}

	@Override
	protected boolean accept(int count) {
		return count >= mimimumAcceptableCount;
	}
	
	public BruteForceResult pack(List<Placement> placements, Container container, com.github.skjolber.packing.impl.PermutationRotationIterator rotator, BooleanSupplier interrupt) {
		return super.pack(placements, container, new SinglePermutationRotationIterator(rotator, permutate, rotate), interrupt);
	}

}
