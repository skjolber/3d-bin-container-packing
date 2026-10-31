package com.github.skjolber.packing.tornado;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.github.skjolber.packing.accelerator.FlatLayout;

/**
 * The kernel: a prebuilt OpenCL C kernel ({@code subtree.cl}, next to this class) in which one work item runs the {@linkplain
 * com.github.skjolber.packing.accelerator.FlatSubtree} of one slot of a window. The state of a slot is in the window arrays, which the host
 * fills with the items to run (and copies back afterwards), so that the memory on the device does not depend on the number of items of the search.
 * <p>
 * The kernel is a port of the flat package, function by function (the search of the orders of an item one at a time, {@code FlatSubtree}, and as one
 * tree, {@code FlatTree}, chosen by {@link #P_TREE}); it is written by hand because the TornadoVM JIT (7.3.1, OpenCL) does not compile the
 * search correctly (it compiles, but the kernel hangs). TornadoVM loads a prebuilt kernel from a file, so the resource is copied to a temporary file once.
 * <p>
 * The parameters of the kernel, after the four of the TornadoVM calling convention, are the layout as ints ({@link #layoutInts(FlatLayout)}), the ints and
 * longs of the problem, the active flags of the slots, the prefixes of the slots, the int and long parameters, the int and long scratch, and the output
 * ints and longs, see {@link TornadoBruteForceAccelerator}.
 */
public final class SubtreeKernel {

	/** the name of the kernel function */
	public static final String ENTRY_POINT = "search";

	/** the index in the ints of the parameters: the length of the prefixes of the items */
	public static final int P_DEPTH = 0;
	/** the index in the ints of the parameters: the number of nodes after which an item pauses */
	public static final int P_BUDGET = 1;
	/** the index in the ints of the parameters: the number of slots, which is the number of work items */
	public static final int P_SLOTS = 2;
	/** the index in the ints of the parameters: 1 to search the orders of an item as one tree ({@code FlatTree}), 0 one order at a time ({@code FlatSubtree}) */
	public static final int P_TREE = 3;
	/** the index in the ints of the parameters: 1 to bound the nodes of a tree item by their free space ({@code FlatSpace}) */
	public static final int P_SPACE = 4;
	public static final int PARAMETER_INTS = 5;

	/** the index in the longs of the parameters: the load volume which a permutation must be able to exceed */
	public static final int P_BOUND = 0;
	/** the index in the longs of the parameters: the best arrangement of the items so far (volume, weight, count), which bounds the tree items */
	public static final int P_SHARED_VOLUME = 1;
	public static final int P_SHARED_WEIGHT = 2;
	public static final int P_SHARED_COUNT = 3;
	public static final int PARAMETER_LONGS = 4;

	private static final String RESOURCE = "subtree.cl";

	private static volatile Path file;

	private SubtreeKernel() {
	}

	/**
	 * @return the offsets of the layout, in the order in which the kernel reads them (the {@code Layout} struct of the kernel)
	 */
	public static int[] layoutInts(FlatLayout layout) {
		return new int[] { layout.n, layout.pMax, layout.frameInts, layout.control, layout.placements, layout.frames, layout.flags, layout.moveX, layout.moveY, layout.moveZ, layout.poolX,
				layout.poolY, layout.poolZ, layout.nextX, layout.nextY, layout.nextZ, layout.headX, layout.headY, layout.headZ, layout.tailX, layout.tailY, layout.tailZ, layout.copyX,
				layout.copyY, layout.copyZ, layout.perm, layout.rotation, layout.placedRotation, layout.bestRotation, layout.typeAt, layout.fit, layout.placedType, layout.remaining, layout.spaces, layout.spaceSizes,
				layout.spaceRemaining, layout.known, layout.witness, layout.spaceChecked, layout.size,
				layout.minArea, layout.minVolume, layout.placedVolume, layout.bestVolume, layout.placedWeight, layout.longSize, layout.types };
	}

	/**
	 * @return the file with the source of the kernel, which TornadoVM compiles for the device
	 */
	public static Path file() {
		Path result = file;
		if(result == null) {
			synchronized (SubtreeKernel.class) {
				result = file;
				if(result == null) {
					result = extract();
					file = result;
				}
			}
		}
		return result;
	}

	private static Path extract() {
		try (InputStream in = SubtreeKernel.class.getResourceAsStream(RESOURCE)) {
			if(in == null) {
				throw new IllegalStateException("Kernel resource not found: " + RESOURCE);
			}
			Path path = Files.createTempFile("packing-subtree", ".cl");
			path.toFile().deleteOnExit();
			Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
			return path;
		} catch (IOException e) {
			throw new UncheckedIOException("Could not write the kernel to a file", e);
		}
	}
}
