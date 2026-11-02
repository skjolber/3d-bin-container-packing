package com.github.skjolber.packing.accelerator;

import java.util.Optional;

import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;

/**
 * Searches all permutations of the boxes of one container, on something other than the packager's own thread: a
 * {@linkplain FlatEngine} on other threads of the CPU, or a device such as a GPU. It replaces the permutation loop of
 * {@linkplain com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager} for box orders which can be searched on the flat model
 * ({@linkplain FlatProblem}).
 * <p>
 * The result must be the one which the packager's own loop would pick: the permutation with the most load volume, then the most load weight, then the most
 * boxes, where the first permutation (in the order of the iterator) wins a tie, or the first permutation which places all boxes. A permutation which
 * cannot be better than another need not be searched. The packager places the winner again to get the positions, and falls back to its own loop when the
 * accelerator returns nothing, or when the replay does not give the winner's count and load volume.
 * <p>
 * An implementation is used by more than one thread at a time, so it must be thread-safe.
 */
public interface BruteForceAccelerator extends AutoCloseable {

	/**
	 * @param problem the boxes of one container
	 * @return true if {@link #search(FlatProblem, long, PackagerInterruptSupplier)} is worth trying for the problem
	 */
	boolean supports(FlatProblem problem);

	/**
	 * Search all permutations.
	 *
	 * @param problem the boxes of one container
	 * @param boundVolume a load volume which a better result must exceed, or zero: a permutation which cannot load more is not searched. This is the load
	 *        volume of the best result of other containers; if no permutation can load at least as much, the winner places no boxes.
	 * @param interrupt the interrupt, which is polled now and then
	 * @return the winner (which places no box if none of the permutations is worth placing), or empty if the accelerator cannot search the problem: the
	 *         packager then searches the permutations itself
	 * @throws PackagerInterruptedException if interrupted
	 */
	Optional<AcceleratorWinner> search(FlatProblem problem, long boundVolume, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException;

	/** Release threads, devices and so on. The accelerator is not used after it is closed. */
	@Override
	default void close() {
	}
}
