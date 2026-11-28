package com.github.skjolber.packing.accelerator;

/** The Java code, which is the reference. */
public final class JavaFlatKernel implements FlatKernel {

	public static final JavaFlatKernel INSTANCE = new JavaFlatKernel();

	private JavaFlatKernel() {
	}

	@Override
	public void clear(FlatLayout layout, int[] s, int dx, int dy, int dz) {
		FlatPoints.clear(layout, s, 0, dx, dy, dz);
	}

	@Override
	public int filterMinimums(FlatLayout layout, int[] s, int level, long minArea, long minVolume) {
		return FlatPoints.filterMinimums(layout, s, 0, level, minArea, minVolume);
	}

	@Override
	public int add(FlatLayout layout, int[] s, int level, int pointIndex, int placementId, long minArea, long minVolume) {
		return FlatPoints.add(layout, s, 0, level, pointIndex, placementId, minArea, minVolume);
	}

	@Override
	public int bestPoint(int[] s, int frame, int dx, int dy, int dz) {
		return FlatSearch.bestPoint(s, frame, dx, dy, dz);
	}

	@Override
	public int searchPermutation(FlatLayout layout, int[] problem, long[] problemLongs, int[] s, long[] sl, int maxPackableCount) {
		return FlatSearch.searchPermutation(layout, problem, problemLongs, s, sl, 0, 0, maxPackableCount);
	}

	@Override
	public void first(int[] problem, int[] permutation, int offset) {
		FlatPermutation.first(problem, permutation, offset);
	}

	@Override
	public int next(int[] permutation, int offset, int n, int maxIndex) {
		return FlatPermutation.next(permutation, offset, n, maxIndex);
	}

	@Override
	public int[] prefixes(int[] problem, int depth, int maxItems) {
		return FlatPermutation.prefixes(problem, depth, maxItems);
	}

	@Override
	public int depth(int[] problem, int minItems, int maxItems) {
		return FlatPermutation.depth(problem, minItems, maxItems);
	}
}
