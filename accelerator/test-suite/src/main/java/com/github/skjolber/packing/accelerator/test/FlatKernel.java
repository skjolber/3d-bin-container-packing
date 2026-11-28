package com.github.skjolber.packing.accelerator.test;

import com.github.skjolber.packing.accelerator.*;

/**
 * The functions of the flat search which the tests call one at a time, so that the same tests run on the Java code and on its ports to native code:
 * the static methods of {@linkplain FlatPoints}, {@linkplain FlatSearch} and {@linkplain FlatPermutation}, on the first scratch of the arrays (base zero).
 * What only reads or writes a value of the scratch ({@link FlatPoints#count}, {@link FlatPoints#setPlacement} and the like) is not here: the layout of the
 * scratch is the same for all, and the tests use the Java methods for it.
 */
public interface FlatKernel {

	/** {@link FlatPoints#clear(FlatLayout, int[], int, int, int, int)} */
	void clear(FlatLayout layout, int[] s, int dx, int dy, int dz);

	/** {@link FlatPoints#filterMinimums(FlatLayout, int[], int, int, long, long)} */
	int filterMinimums(FlatLayout layout, int[] s, int level, long minArea, long minVolume);

	/** {@link FlatPoints#add(FlatLayout, int[], int, int, int, int, long, long)} */
	int add(FlatLayout layout, int[] s, int level, int pointIndex, int placementId, long minArea, long minVolume);

	/** {@link FlatSearch#bestPoint(int[], int, int, int, int)} */
	int bestPoint(int[] s, int frame, int dx, int dy, int dz);

	/** {@link FlatSearch#searchPermutation(FlatLayout, int[], long[], int[], long[], int, int, int)}; the layout is that of the problem's number of types */
	int searchPermutation(FlatLayout layout, int[] problem, long[] problemLongs, int[] s, long[] sl, int maxPackableCount);

	/** {@link FlatPermutation#first(int[], int[], int)} */
	void first(int[] problem, int[] permutation, int offset);

	/** {@link FlatPermutation#next(int[], int, int, int)} */
	int next(int[] permutation, int offset, int n, int maxIndex);

	/** {@link FlatPermutation#prefixes(int[], int, int)} */
	int[] prefixes(int[] problem, int depth, int maxItems);

	/** {@link FlatPermutation#depth(int[], int, int)} */
	int depth(int[] problem, int minItems, int maxItems);
}
