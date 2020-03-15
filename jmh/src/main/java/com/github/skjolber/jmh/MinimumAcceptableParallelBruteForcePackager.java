package com.github.skjolber.jmh;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.ParallelBruteForcePackager;
import com.github.skjolber.packing.Placement;
import com.github.skjolber.packing.impl.BruteForceResult;
import com.github.skjolber.packing.impl.PermutationRotationIterator;

public class MinimumAcceptableParallelBruteForcePackager extends ParallelBruteForcePackager {

	private final int mimimumAcceptableCount;
	private final boolean permutate;
	private final boolean rotate;
	
	public MinimumAcceptableParallelBruteForcePackager(List<Container> containers, int threads, int checkpointsPerDeadlineCheck, int mimimumAcceptableCount, boolean permutate, boolean rotate) {
		super(containers, threads, checkpointsPerDeadlineCheck);
		
		this.mimimumAcceptableCount = mimimumAcceptableCount;
		this.permutate = permutate;
		this.rotate = rotate;
	}
	
	public MinimumAcceptableParallelBruteForcePackager(List<Container> containers, int threads, boolean rotate3D, boolean binarySearch, int checkpointsPerDeadlineCheck, int mimimumAcceptableCount, boolean permutate, boolean rotate) {
		super(containers, threads, rotate3D, binarySearch, checkpointsPerDeadlineCheck);
		
		this.mimimumAcceptableCount = mimimumAcceptableCount;
		this.permutate = permutate;
		this.rotate = rotate;
	}
	
	public MinimumAcceptableParallelBruteForcePackager(List<Container> containers, ExecutorService executorService, int threads, boolean rotate3D, boolean binarySearch, int checkpointsPerDeadlineCheck, int mimimumAcceptableCount, boolean permutate, boolean rotate) {
		super(containers, executorService, threads, rotate3D, binarySearch, checkpointsPerDeadlineCheck);
		
		this.mimimumAcceptableCount = mimimumAcceptableCount;
		this.permutate = permutate;
		this.rotate = rotate;
	}


	@Override
	protected boolean accept(int count) {
		return count >= mimimumAcceptableCount;
	}
	
	public BruteForceResult pack(List<Placement> placements, Container container, PermutationRotationIterator rotator, BooleanSupplier interrupt) {
		return super.pack(placements, container, new SinglePermutationRotationIterator(rotator, permutate, rotate), interrupt);
	}
}
