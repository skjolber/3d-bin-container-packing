package com.github.skjolber.packing.packer.bruteforce.flat;

/**
 * The search of all box orders as one tree, on the calling thread: one item of {@linkplain FlatTreeItem} with no prefix, run to the end. The winner is
 * the winner of the permutation loop ({@linkplain FlatSubtree}); see FlatTreeItem for the tree and its pruning.
 */
public final class FlatJointSearch {

	private FlatJointSearch() {
	}

	/**
	 * Search all box orders.
	 *
	 * @param problem the boxes of one container
	 * @param boundVolume a load volume which a better result must exceed, or zero
	 * @param maxPoints the most free points kept per level
	 * @return the winner (which places no box if none of the arrangements is worth placing), or null if a level needed more free points than maxPoints
	 */
	public static AcceleratorWinner search(FlatProblem problem, long boundVolume, int maxPoints) {
		int n = problem.n();
		FlatLayout layout = new FlatLayout(n, maxPoints);
		int[] s = layout.newScratch();
		FlatItems items = new FlatItems(n, 1, FlatTreeItem.outInts(n));
		int status;
		do {
			status = FlatTreeItem.run(layout, problem.ints(), problem.longs(), 0, new int[0], 0, s, 0, items.out(), 0, items.outLongs(), 0, boundVolume, 0L, 0, Integer.MAX_VALUE,
					Integer.MAX_VALUE);
		} while (status == FlatSubtree.PAUSED);
		if(status == FlatSubtree.OVERFLOW) {
			return null;
		}
		return items.reduce();
	}
}
