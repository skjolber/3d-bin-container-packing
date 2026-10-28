package com.github.skjolber.packing.tornado;

import java.util.List;
import java.util.Optional;

import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.packer.bruteforce.flat.AcceleratorWinner;
import com.github.skjolber.packing.packer.bruteforce.flat.BruteForceAccelerator;
import com.github.skjolber.packing.packer.bruteforce.flat.FlatItems;
import com.github.skjolber.packing.packer.bruteforce.flat.FlatLayout;
import com.github.skjolber.packing.packer.bruteforce.flat.FlatPermutation;
import com.github.skjolber.packing.packer.bruteforce.flat.FlatProblem;
import com.github.skjolber.packing.packer.bruteforce.flat.FlatSubtree;

import uk.ac.manchester.tornado.api.AccessorParameters;
import uk.ac.manchester.tornado.api.GridScheduler;
import uk.ac.manchester.tornado.api.ImmutableTaskGraph;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.WorkerGrid1D;
import uk.ac.manchester.tornado.api.common.Access;
import uk.ac.manchester.tornado.api.common.TornadoDevice;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.api.exceptions.TornadoExecutionPlanException;

/**
 * A {@linkplain BruteForceAccelerator} which searches the permutations of the fast brute force packager on a TornadoVM device. Experimental.
 * <p>
 * The permutations are split in items by their first boxes, as for {@linkplain com.github.skjolber.packing.packer.bruteforce.flat.FlatEngine}. A window of
 * items at a time runs on the device, one item for each work item, each with its own scratch memory, for a number of search nodes at most. The host then
 * reads the state of the items, polls the interrupt, and runs the next round with the items which are not done. The winner is the same as the one of the
 * CPU engine, which the packager replays and verifies.
 * <p>
 * The accelerator gives up (the packager then searches the permutations itself) when there is no device, when a search needs more free points than the
 * layout holds, and when the device fails; see {@link #getLastFailure()}.
 */
public final class TornadoBruteForceAccelerator implements BruteForceAccelerator {

	public static final int DEFAULT_MAX_POINTS = 128;
	public static final int DEFAULT_NODE_BUDGET = 1_000;
	public static final int DEFAULT_WINDOW = 2048;
	/** one work item per work group: the search diverges so much that lanes of the same wavefront only wait for each other */
	public static final int DEFAULT_LOCAL_WORK_SIZE = 1;
	public static final int DEFAULT_TARGET_ITEMS = 8192;
	public static final int DEFAULT_MAX_ITEMS = 1 << 16;
	public static final int DEFAULT_MIN_BOXES = 8;
	public static final int DEFAULT_MAX_BOXES = 64;

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		private DeviceSelector selector = DeviceSelector.newBuilder().build();
		private int maxPoints = DEFAULT_MAX_POINTS;
		private int nodeBudget = DEFAULT_NODE_BUDGET;
		private int window = DEFAULT_WINDOW;
		private int localWorkSize = DEFAULT_LOCAL_WORK_SIZE;
		private int targetItems = DEFAULT_TARGET_ITEMS;
		private int maxItems = DEFAULT_MAX_ITEMS;
		private int minBoxes = DEFAULT_MIN_BOXES;
		private int maxBoxes = DEFAULT_MAX_BOXES;

		/**
		 * @param selector how to choose the device, by default the OpenCL GPU with the most compute throughput
		 * @return this builder
		 */
		public Builder withDeviceSelector(DeviceSelector selector) {
			if(selector == null) {
				throw new IllegalArgumentException("Expected a device selector");
			}
			this.selector = selector;
			return this;
		}

		/**
		 * @param maxPoints the most free points which a search keeps at once; the accelerator gives up on a problem which needs more. Each work item has memory for
		 *        this many points at each level.
		 * @return this builder
		 */
		public Builder withMaxPoints(int maxPoints) {
			if(maxPoints < 1) {
				throw new IllegalArgumentException("Expected at least one point, got " + maxPoints);
			}
			this.maxPoints = maxPoints;
			return this;
		}

		/**
		 * @param nodeBudget the number of search nodes after which an item pauses, so that no run of the kernel is long, the interrupt is polled, and the load
		 *        volumes found by the items so far bound the next round (a small budget means fewer nodes in all, but more rounds)
		 * @return this builder
		 */
		public Builder withNodeBudget(int nodeBudget) {
			if(nodeBudget < 1) {
				throw new IllegalArgumentException("Expected a positive node budget, got " + nodeBudget);
			}
			this.nodeBudget = nodeBudget;
			return this;
		}

		/**
		 * @param window the most items which run on the device at once; each needs its own scratch memory on the device
		 * @return this builder
		 */
		public Builder withWindow(int window) {
			if(window < 1) {
				throw new IllegalArgumentException("Expected at least one item in a window, got " + window);
			}
			this.window = window;
			return this;
		}

		/**
		 * @param localWorkSize the number of work items in a work group of the device, or 0 to let the runtime choose. Each work item is a search of its own,
		 *        with data-dependent branches, so the work items of a wavefront mostly wait for each other: a work group of one measured 2.5 times faster than
		 *        the choice of the runtime on a Radeon RX 7600.
		 * @return this builder
		 */
		public Builder withLocalWorkSize(int localWorkSize) {
			if(localWorkSize < 0) {
				throw new IllegalArgumentException("Expected a local work size of zero or more, got " + localWorkSize);
			}
			this.localWorkSize = localWorkSize;
			return this;
		}

		/**
		 * @param targetItems the number of items to split the permutations in, at least
		 * @return this builder
		 */
		public Builder withTargetItems(int targetItems) {
			if(targetItems < 1) {
				throw new IllegalArgumentException("Expected at least one item, got " + targetItems);
			}
			this.targetItems = targetItems;
			return this;
		}

		/**
		 * @param maxItems the most items: a problem with more item prefixes than this is not searched
		 * @return this builder
		 */
		public Builder withMaxItems(int maxItems) {
			if(maxItems < 1) {
				throw new IllegalArgumentException("Expected at least one item, got " + maxItems);
			}
			this.maxItems = maxItems;
			return this;
		}

		/**
		 * @param minBoxes the fewest boxes of a problem worth the device: with fewer, the packager's own loop is quicker
		 * @return this builder
		 */
		public Builder withMinBoxes(int minBoxes) {
			if(minBoxes < 0) {
				throw new IllegalArgumentException("Expected zero or more boxes, got " + minBoxes);
			}
			this.minBoxes = minBoxes;
			return this;
		}

		/**
		 * @param maxBoxes the most boxes of a problem which the accelerator searches
		 * @return this builder
		 */
		public Builder withMaxBoxes(int maxBoxes) {
			if(maxBoxes < 1 || maxBoxes > FlatLayout.MAX_PLACEMENTS) {
				throw new IllegalArgumentException("Expected 1 to " + FlatLayout.MAX_PLACEMENTS + " boxes, got " + maxBoxes);
			}
			this.maxBoxes = maxBoxes;
			return this;
		}

		public TornadoBruteForceAccelerator build() {
			return new TornadoBruteForceAccelerator(selector, maxPoints, nodeBudget, window, localWorkSize, targetItems, maxItems, minBoxes, maxBoxes);
		}
	}

	private final DeviceSelector selector;
	private final int maxPoints;
	private final int nodeBudget;
	private final int window;
	private final int localWorkSize;
	private final int targetItems;
	private final int maxItems;
	private final int minBoxes;
	private final int maxBoxes;

	private volatile Optional<DeviceInfo> device;
	private volatile Throwable lastFailure;
	private volatile boolean failed;
	private volatile boolean closed;

	/** the compiled kernel and the device memory of the last shape of problem, or null */
	private Session session;

	private long searches;
	private long rounds;
	private long nanosOnDevice;

	private TornadoBruteForceAccelerator(DeviceSelector selector, int maxPoints, int nodeBudget, int window, int localWorkSize, int targetItems, int maxItems, int minBoxes,
			int maxBoxes) {
		this.selector = selector;
		this.maxPoints = maxPoints;
		this.nodeBudget = nodeBudget;
		this.window = window;
		this.localWorkSize = localWorkSize;
		this.targetItems = targetItems;
		this.maxItems = maxItems;
		this.minBoxes = minBoxes;
		this.maxBoxes = maxBoxes;
	}

	/**
	 * @return the chosen device, or empty if there is no usable TornadoVM runtime, or no device matches. The runtime is asked once, when needed.
	 */
	public Optional<DeviceInfo> getDevice() {
		Optional<DeviceInfo> result = device;
		if(result == null) {
			List<DeviceInfo> devices = TornadoDevices.discover();
			result = selector.select(devices);
			device = result;
		}
		return result;
	}

	/**
	 * @return the failure which made the accelerator stop using the device, or null
	 */
	public Throwable getLastFailure() {
		return lastFailure;
	}

	/** @return the number of searches which ran on the device */
	public long getSearches() {
		return searches;
	}

	/** @return the number of times the kernel ran */
	public long getRounds() {
		return rounds;
	}

	/** @return the time spent in the executions of the kernel, including transfers */
	public long getNanosOnDevice() {
		return nanosOnDevice;
	}

	@Override
	public boolean supports(FlatProblem problem) {
		if(failed || closed) {
			return false;
		}
		int n = problem.n();
		return n >= minBoxes && n <= maxBoxes && getDevice().isPresent();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The device runs one search at a time: searches from other threads wait.
	 */
	@Override
	public synchronized Optional<AcceleratorWinner> search(FlatProblem problem, long boundVolume, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		if(!supports(problem)) {
			return Optional.empty();
		}
		int[] ints = problem.ints();
		int depth = FlatPermutation.depth(ints, targetItems, maxItems);
		if(depth == 0) {
			return Optional.empty();
		}
		int[] prefixes = FlatPermutation.prefixes(ints, depth, maxItems);
		if(prefixes == null) {
			return Optional.empty();
		}
		DeviceInfo info = getDevice().get();
		try {
			return Optional.ofNullable(run(info, problem, boundVolume, interrupt, depth, prefixes));
		} catch (PackagerInterruptedException e) {
			throw e;
		} catch (Exception | LinkageError e) {
			// the device or the runtime failed: leave the work to the packager, now and for the next searches
			lastFailure = e;
			failed = true;
			closeSession();
			return Optional.empty();
		}
	}

	/** Release the device memory and the compiled kernel. */
	@Override
	public synchronized void close() {
		closed = true;
		closeSession();
	}

	private void closeSession() {
		Session session = this.session;
		this.session = null;
		if(session != null) {
			try {
				session.plan.close();
			} catch (TornadoExecutionPlanException e) {
				if(lastFailure == null) {
					lastFailure = e;
				}
			}
		}
	}

	/**
	 * The compiled kernel, its execution plan and the arrays bound to it: one for each shape of problem (the number of boxes and the sizes of the problem
	 * arrays), reused by the searches of that shape, so that the kernel is compiled and the scratch allocated once for all the containers of a packing.
	 */
	private static final class Session {

		final int n;
		final int problemInts;
		final int problemLongs;
		final FlatLayout layout;
		final int slots;
		final int outInts;

		final int[] problem;
		final long[] longs;
		final int[] active;
		final int[] prefixes;
		final int[] parameters;
		final long[] longParameters;
		final int[] out;
		final long[] outLongs;
		final TornadoExecutionPlan plan;

		Session(TornadoDevice device, int n, int problemInts, int problemLongs, int maxPoints, int window, int localWorkSize) {
			this.n = n;
			this.problemInts = problemInts;
			this.problemLongs = problemLongs;
			this.layout = new FlatLayout(n, maxPoints);
			// each slot has the scratch of the layout; keep the scratch well below the largest allocation of the device
			long slotBytes = 4L * layout.size + 8L * layout.longSize;
			long maxSlots = Math.max(1L, (device.getMaxAllocMemory() / 2) / slotBytes);
			this.slots = (int)Math.min(window, maxSlots);
			this.outInts = FlatSubtree.outInts(n);

			int[] layoutInts = SubtreeKernel.layoutInts(layout);
			problem = new int[problemInts];
			longs = new long[problemLongs];
			active = new int[slots];
			prefixes = new int[slots * n];
			parameters = new int[SubtreeKernel.PARAMETER_INTS];
			longParameters = new long[SubtreeKernel.PARAMETER_LONGS];
			int[] s = new int[slots * layout.size];
			long[] sl = new long[slots * layout.longSize];
			out = new int[slots * outInts];
			outLongs = new long[slots * FlatSubtree.OUT_LONGS];
			parameters[SubtreeKernel.P_SLOTS] = slots;

			// the parameters of the kernel, in its order
			AccessorParameters accessors = new AccessorParameters(11);
			accessors.set(0, layoutInts, Access.READ_ONLY);
			accessors.set(1, problem, Access.READ_ONLY);
			accessors.set(2, longs, Access.READ_ONLY);
			accessors.set(3, active, Access.READ_ONLY);
			accessors.set(4, prefixes, Access.READ_ONLY);
			accessors.set(5, parameters, Access.READ_ONLY);
			accessors.set(6, longParameters, Access.READ_ONLY);
			accessors.set(7, s, Access.READ_WRITE);
			accessors.set(8, sl, Access.READ_WRITE);
			accessors.set(9, out, Access.READ_WRITE);
			accessors.set(10, outLongs, Access.READ_WRITE);

			// the scratch stays on the device; the problem and the window go with each run
			TaskGraph graph = new TaskGraph("bruteForce").transferToDevice(DataTransferMode.FIRST_EXECUTION, layoutInts, s, sl)
					.transferToDevice(DataTransferMode.EVERY_EXECUTION, problem, longs, active, prefixes, parameters, longParameters, out, outLongs)
					.prebuiltTask("search", SubtreeKernel.ENTRY_POINT, SubtreeKernel.file().toString(), accessors)
					.transferToHost(DataTransferMode.EVERY_EXECUTION, out, outLongs);
			ImmutableTaskGraph immutable = graph.snapshot();
			// one work item for each slot
			WorkerGrid1D grid = new WorkerGrid1D(slots);
			if(localWorkSize > 0) {
				grid.setLocalWork(localWorkSize, 1, 1);
			}
			GridScheduler scheduler = new GridScheduler("bruteForce.search", grid);
			plan = new TornadoExecutionPlan(immutable);
			plan.withDevice(device).withGridScheduler(scheduler);
		}

		boolean matches(int n, int problemInts, int problemLongs) {
			return this.n == n && this.problemInts == problemInts && this.problemLongs == problemLongs;
		}
	}

	private Session session(DeviceInfo info, FlatProblem problem) {
		int n = problem.n();
		int problemInts = problem.ints().length;
		int problemLongs = problem.longs().length;
		Session session = this.session;
		if(session == null || !session.matches(n, problemInts, problemLongs)) {
			closeSession();
			TornadoDevice tornadoDevice = TornadoExecutionPlan.getDevice(info.backendIndex(), info.deviceIndex());
			session = new Session(tornadoDevice, n, problemInts, problemLongs, maxPoints, window, localWorkSize);
			this.session = session;
		}
		return session;
	}

	private AcceleratorWinner run(DeviceInfo info, FlatProblem problem, long boundVolume, PackagerInterruptSupplier interrupt, int depth, int[] prefixes) throws Exception {
		final Session session = session(info, problem);
		final int n = session.n;
		final int slots = session.slots;
		final int outInts = session.outInts;
		final FlatItems flat = new FlatItems(n, prefixes.length / n);
		final int[] active = session.active;
		final int[] out = session.out;
		final long[] outLongs = session.outLongs;
		final long[] longParameters = session.longParameters;

		System.arraycopy(problem.ints(), 0, session.problem, 0, session.problemInts);
		System.arraycopy(problem.longs(), 0, session.longs, 0, session.problemLongs);
		session.parameters[SubtreeKernel.P_DEPTH] = depth;
		session.parameters[SubtreeKernel.P_BUDGET] = nodeBudget;

		int[] pending = new int[flat.items()];
		long bound = boundVolume;
		searches++;
		while (true) {
			int fullItem = flat.fullItem();
			int pendingCount = flat.pending(fullItem, pending);
			if(pendingCount == 0) {
				break;
			}
			for (int start = 0; start < pendingCount; start += slots) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				int count = Math.min(slots, pendingCount - start);
				for (int slot = 0; slot < count; slot++) {
					int item = pending[start + slot];
					active[slot] = 1;
					System.arraycopy(prefixes, item * n, session.prefixes, slot * n, n);
					System.arraycopy(flat.out(), item * outInts, out, slot * outInts, outInts);
					System.arraycopy(flat.outLongs(), item * FlatSubtree.OUT_LONGS, outLongs, slot * FlatSubtree.OUT_LONGS, FlatSubtree.OUT_LONGS);
				}
				for (int slot = count; slot < slots; slot++) {
					active[slot] = 0;
				}
				bound = Math.max(bound, flat.bestVolume());
				longParameters[SubtreeKernel.P_BOUND] = bound;

				long startNanos = System.nanoTime();
				session.plan.execute();
				nanosOnDevice += System.nanoTime() - startNanos;
				rounds++;

				boolean overflow = false;
				for (int slot = 0; slot < count; slot++) {
					int item = pending[start + slot];
					System.arraycopy(out, slot * outInts, flat.out(), item * outInts, outInts);
					System.arraycopy(outLongs, slot * FlatSubtree.OUT_LONGS, flat.outLongs(), item * FlatSubtree.OUT_LONGS, FlatSubtree.OUT_LONGS);
					if(out[slot * outInts + FlatSubtree.OUT_STATUS] == FlatSubtree.OVERFLOW) {
						overflow = true;
					}
				}
				if(overflow) {
					return null;
				}
				if(flat.fullItem() < fullItem) {
					// an earlier item places all boxes: the later items of this round need not run
					break;
				}
			}
		}
		return flat.reduce();
	}
}
