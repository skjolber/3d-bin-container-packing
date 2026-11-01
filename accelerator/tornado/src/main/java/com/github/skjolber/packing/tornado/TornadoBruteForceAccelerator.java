package com.github.skjolber.packing.tornado;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.accelerator.AcceleratorWinner;
import com.github.skjolber.packing.accelerator.BruteForceAccelerator;
import com.github.skjolber.packing.accelerator.FlatItems;
import com.github.skjolber.packing.accelerator.FlatLayout;
import com.github.skjolber.packing.accelerator.FlatPermutation;
import com.github.skjolber.packing.accelerator.FlatProblem;
import com.github.skjolber.packing.accelerator.FlatSubtree;

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
 * The permutations are split in items by their first boxes, as for {@linkplain com.github.skjolber.packing.accelerator.FlatEngine}. A window of
 * items at a time runs on the device, one item for each work item, each with its own scratch memory, for a number of search nodes at most (a budget which
 * doubles each round up to the node budget). Each item is searched as one tree by default (as {@code FlatTree}, with a bound table computed once per
 * problem; {@link Builder#withTree(boolean)} false searches the orders one at a time), and the items share their best arrangement through the host. The kernel
 * is compiled once per accelerator. A tree item bounds its nodes by their free space ({@link Builder#withFreeSpace(boolean)}, by default for the exact
 * winner only), and under any best a tree item which runs on while most slots are idle is split into its children on the host ({@link Builder#withSplit(boolean)},
 * {@link #getSplits()}). The host then
 * reads the state of the items, polls the interrupt, and runs the next round with the items which are not done. The winner is the same as the one of the
 * CPU engine, which the packager replays and verifies.
 * <p>
 * The accelerator gives up (the packager then searches the permutations itself) when there is no device, when a search needs more free points than the
 * layout holds, and when the device fails; see {@link #getLastFailure()}.
 */
public final class TornadoBruteForceAccelerator implements BruteForceAccelerator {

	public static final int DEFAULT_MAX_POINTS = 64;
	public static final int DEFAULT_NODE_BUDGET = 250;
	public static final int DEFAULT_WINDOW = 8192;
	/** one work item per work group: the search diverges so much that lanes of the same wavefront only wait for each other */
	public static final int DEFAULT_LOCAL_WORK_SIZE = 1;
	public static final int DEFAULT_TARGET_ITEMS = 8192;
	public static final int DEFAULT_MAX_ITEMS = 1 << 16;
	public static final int DEFAULT_MIN_BOXES = 8;
	public static final int DEFAULT_MAX_BOXES = 64;
	public static final boolean DEFAULT_TREE = true;
	public static final boolean DEFAULT_SPLIT = true;
	/** a tree item is split into its children after this many rounds, when fewer than a quarter of the slots are active and any best arrangement will do */
	private static final int SPLIT_ROUNDS = 4;
	/** the most rotations of a box */
	private static final int MAX_ROTATIONS = 6;
	/** the most a round's node budget grows when few tree items are left */
	private static final int MAX_BUDGET_FACTOR = 64;
	/** the node budget of the first round of a tree search, doubled each round up to the budget */
	private static final int FIRST_BUDGET = 32;

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
		private boolean tree = DEFAULT_TREE;
		private Boolean freeSpace = null;
		private boolean split = DEFAULT_SPLIT;

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

		/**
		 * @param tree true to search the orders of an item as one tree ({@code FlatTree}: each prefix of placements once, with bounds), false one order at
		 *        a time ({@code FlatSubtree})
		 * @return this builder
		 */
		public Builder withTree(boolean tree) {
			this.tree = tree;
			return this;
		}

		/**
		 * @param freeSpace whether a tree item bounds its nodes by their free space (maximal empty spaces, as {@code FlatSpace}); by default for the
		 *        loop's winner (not under {@code FlatProblem.ANY_BEST}) and for the search of every box at every free point ({@code FlatProblem.ALL_POINTS}),
		 *        where it cuts the most: at the best point only with ties pruned the bound table leaves it little to cut for its cost
		 * @return this builder
		 */
		public Builder withFreeSpace(boolean freeSpace) {
			this.freeSpace = freeSpace;
			return this;
		}

		/**
		 * @param split whether a tree item which runs on while most slots are idle is split into its children (the device's form of work stealing),
		 *        when any best arrangement will do; the default
		 * @return this builder
		 */
		public Builder withSplit(boolean split) {
			this.split = split;
			return this;
		}

		public TornadoBruteForceAccelerator build() {
			return new TornadoBruteForceAccelerator(selector, maxPoints, nodeBudget, window, localWorkSize, targetItems, maxItems, minBoxes, maxBoxes, tree, freeSpace, split);
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
	private final boolean tree;
	private final Boolean freeSpace;
	private final boolean split;

	private volatile Optional<DeviceInfo> device;
	private volatile Throwable lastFailure;
	private volatile boolean failed;
	private volatile boolean closed;

	/** the compiled kernel and the device memory, made by the first search (and again by a search of more boxes than it holds), or null */
	/** the device runs one search at a time; held by the search which runs, and while closing */
	private final ReentrantLock deviceLock = new ReentrantLock();
	private Session session;

	private long searches;
	private long rounds;
	private long splits;
	private long nanosOnDevice;

	private TornadoBruteForceAccelerator(DeviceSelector selector, int maxPoints, int nodeBudget, int window, int localWorkSize, int targetItems, int maxItems, int minBoxes,
			int maxBoxes, boolean tree, Boolean freeSpace, boolean split) {
		this.tree = tree;
		this.freeSpace = freeSpace;
		this.split = split;
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

	/** @return the number of tree items split into their children */
	public long getSplits() {
		return splits;
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
		// only the tree kernel takes all points (the search of BruteForcePackager)
		return n >= minBoxes && n <= maxBoxes && (tree || !problem.isAllPoints()) && getDevice().isPresent();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The device runs one search at a time: searches from other threads wait for it, polling their interrupt, so a search which is interrupted while it
	 * waits (one which lost its race in {@link com.github.skjolber.packing.accelerator.AcceleratorMode#AUTO}) never reaches the device.
	 */
	@Override
	public Optional<AcceleratorWinner> search(FlatProblem problem, long boundVolume, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
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
		acquireDevice(interrupt);
		try {
			if(!supports(problem)) {
				// closed or failed while waiting
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
		} finally {
			deviceLock.unlock();
		}
	}

	/** Wait for the device, polling the interrupt. */
	private void acquireDevice(PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		try {
			while (!deviceLock.tryLock(1, TimeUnit.MILLISECONDS)) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new PackagerInterruptedException();
		}
	}

	/** Release the device memory and the compiled kernel. */
	@Override
	public void close() {
		deviceLock.lock();
		try {
			closed = true;
			closeSession();
		} finally {
			deviceLock.unlock();
		}
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
	 * The compiled kernel, its execution plan and the arrays bound to it, sized for a number of boxes and of types (with the most rotations for them, and
	 * the largest bound table), so that the kernel is compiled and the scratch allocated once for all the searches of up to that many boxes: the
	 * kernel reads the layout of a search and the sizes of its problem from the arrays, and the arrays of a smaller problem are the first part of
	 * these. The number of boxes is the first problem's rounded up (to {@link #BOXES_STEP}), and a larger problem makes a larger session once.
	 */
	private static final class Session {

		static final int BOXES_STEP = 8;

		final int maxBoxes;
		final int maxTypes;
		final int maxPoints;
		final int slots;

		final int[] layoutInts;
		final int[] problem;
		final long[] longs;
		/** per slot: 0 when idle, else the depth of its item plus one (the children of a split item have longer prefixes) */
		final int[] active;
		final int[] prefixes;
		final int[] parameters;
		final long[] longParameters;
		final int[] out;
		final long[] outLongs;
		final TornadoExecutionPlan plan;

		Session(TornadoDevice device, int maxBoxes, int maxTypes, int maxPoints, int window, int localWorkSize) {
			this.maxBoxes = maxBoxes;
			this.maxTypes = maxTypes;
			this.maxPoints = maxPoints;
			FlatLayout layout = new FlatLayout(maxBoxes, maxPoints, maxTypes);
			// each slot has the scratch of the layout; keep the scratch well below the largest allocation of the device
			long slotBytes = 4L * layout.size + 8L * layout.longSize;
			long maxSlots = Math.max(1L, (device.getMaxAllocMemory() / 2) / slotBytes);
			this.slots = (int)Math.min(window, maxSlots);
			int outInts = FlatSubtree.outInts(maxBoxes);

			layoutInts = new int[SubtreeKernel.layoutInts(layout).length];
			problem = new int[FlatProblem.radix(maxTypes, maxTypes * MAX_ROTATIONS) + maxTypes];
			longs = new long[FlatProblem.bounds(maxTypes) + 3 * FlatProblem.MAX_KEYS];
			active = new int[slots];
			prefixes = new int[slots * maxBoxes];
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

			// the scratch stays on the device; the layout, the problem and the window go with each run
			TaskGraph graph = new TaskGraph("bruteForce").transferToDevice(DataTransferMode.FIRST_EXECUTION, s, sl)
					.transferToDevice(DataTransferMode.EVERY_EXECUTION, layoutInts, problem, longs, active, prefixes, parameters, longParameters, out, outLongs)
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

		/** @return true if the problem fits the arrays */
		boolean fits(FlatProblem problem) {
			return problem.n() <= maxBoxes && problem.types() <= maxTypes && problem.ints().length <= this.problem.length && problem.longs().length <= longs.length;
		}
	}

	private Session session(DeviceInfo info, FlatProblem problem) {
		Session session = this.session;
		if(session == null || !session.fits(problem)) {
			closeSession();
			int boxes = Math.min(maxBoxes, (problem.n() + Session.BOXES_STEP - 1) / Session.BOXES_STEP * Session.BOXES_STEP);
			// the types of a packing stay while its boxes are placed, so later containers have more types than boxes
			int types = (problem.types() + Session.BOXES_STEP - 1) / Session.BOXES_STEP * Session.BOXES_STEP;
			TornadoDevice tornadoDevice = TornadoExecutionPlan.getDevice(info.backendIndex(), info.deviceIndex());
			session = new Session(tornadoDevice, boxes, types, maxPoints, window, localWorkSize);
			this.session = session;
		}
		return session;
	}

	private AcceleratorWinner run(DeviceInfo info, FlatProblem problem, long boundVolume, PackagerInterruptSupplier interrupt, int depth, int[] prefixes) throws Exception {
		final Session session = session(info, problem);
		if(!session.fits(problem)) {
			return null;
		}
		final int n = problem.n();
		final int slots = session.slots;
		final int outInts = FlatSubtree.outInts(n);
		final FlatItems flat = new FlatItems(n, prefixes.length / n);
		int items = flat.items();
		final boolean anyBest = problem.ints()[FlatProblem.ANY_BEST] != 0;
		final int[] active = session.active;
		final int[] out = session.out;
		final long[] outLongs = session.outLongs;
		final long[] longParameters = session.longParameters;

		FlatLayout layout = new FlatLayout(n, maxPoints, problem.types());
		int[] layoutInts = SubtreeKernel.layoutInts(layout);
		System.arraycopy(layoutInts, 0, session.layoutInts, 0, layoutInts.length);
		Arrays.fill(session.problem, 0);
		System.arraycopy(problem.ints(), 0, session.problem, 0, problem.ints().length);
		System.arraycopy(problem.longs(), 0, session.longs, 0, problem.longs().length);
		session.parameters[SubtreeKernel.P_DEPTH] = depth;
		session.parameters[SubtreeKernel.P_BUDGET] = nodeBudget;
		session.parameters[SubtreeKernel.P_TREE] = tree ? 1 : 0;
		session.parameters[SubtreeKernel.P_SPACE] = (freeSpace != null ? freeSpace : !anyBest || problem.isAllPoints()) ? 1 : 0;

		// the items in the order they are run; a tree item keeps its slot (its state is the scratch of the slot) until it is done, or until it is
		// split: its children are new items at the end
		int[] order = FlatItems.order(problem.longs(), prefixes, n, depth, anyBest);
		int[] itemDepth = new int[items];
		Arrays.fill(itemDepth, depth);
		final int[] slotItem = new int[slots];
		final int[] slotRounds = new int[slots];
		Arrays.fill(slotItem, -1);
		final boolean splitting = tree && anyBest && split;
		int next = 0;
		int round = 0;
		long bound = boundVolume;
		final long[] best = new long[3];
		searches++;
		while (true) {
			int fullItem = flat.fullItem();
			if(anyBest && fullItem < items) {
				// any arrangement which places all boxes will do
				break;
			}
			int count = 0;
			if(tree) {
				int busy = 0;
				for (int slot = 0; slot < slots; slot++) {
					if(slotItem[slot] != -1 && !flat.isDone(slotItem[slot]) && slotItem[slot] <= fullItem) {
						busy++;
					}
				}
				if(splitting && busy > 0 && busy * 4 < slots) {
					// the items which ran on for several rounds while most slots were idle: their best so far stays, and the search below their prefix
					// goes on in their children, one per remaining type, which run side by side
					for (int slot = 0; slot < slots; slot++) {
						int item = slotItem[slot];
						if(item != -1 && !flat.isDone(item) && slotRounds[slot] >= SPLIT_ROUNDS && itemDepth[item] < n - 1) {
							int childDepth = itemDepth[item] + 1;
							int[] remaining = new int[problem.types()];
							for (int t = 0; t < remaining.length; t++) {
								remaining[t] = problem.ints()[FlatProblem.HEADER + t];
							}
							for (int i = 0; i < itemDepth[item]; i++) {
								remaining[prefixes[item * n + i]]--;
							}
							int children = 0;
							for (int t = 0; t < remaining.length; t++) {
								if(remaining[t] > 0) {
									children++;
								}
							}
							int first = flat.add(children);
							prefixes = Arrays.copyOf(prefixes, (items + children) * n);
							itemDepth = Arrays.copyOf(itemDepth, items + children);
							order = Arrays.copyOf(order, items + children);
							int child = first;
							for (int t = 0; t < remaining.length; t++) {
								if(remaining[t] > 0) {
									// the child's prefix: the parent's, the type, and the rest of the parent's first order without one box of the type
									int at = child * n;
									for (int i = 0; i < itemDepth[item]; i++) {
										prefixes[at + i] = prefixes[item * n + i];
									}
									prefixes[at + itemDepth[item]] = t;
									int index = itemDepth[item] + 1;
									boolean skipped = false;
									for (int i = itemDepth[item]; i < n; i++) {
										int type = prefixes[item * n + i];
										if(type == t && !skipped) {
											skipped = true;
										} else {
											prefixes[at + index++] = type;
										}
									}
									itemDepth[child] = childDepth;
									order[child] = child;
									child++;
								}
							}
							items += children;
							flat.setDone(item);
							slotItem[slot] = -1;
							splits++;
						}
					}
				}
				for (int slot = 0; slot < slots; slot++) {
					if(slotItem[slot] != -1 && (flat.isDone(slotItem[slot]) || slotItem[slot] > fullItem)) {
						slotItem[slot] = -1;
					}
					if(slotItem[slot] == -1) {
						while (next < items && (flat.isDone(order[next]) || order[next] > fullItem)) {
							next++;
						}
						if(next < items) {
							slotItem[slot] = order[next++];
							slotRounds[slot] = 0;
						}
					}
					if(slotItem[slot] != -1) {
						count++;
						slotRounds[slot]++;
					}
				}
			} else {
				// the items which are not done, before the first which places all boxes, in their order
				next = 0;
				for (int slot = 0; slot < slots; slot++) {
					slotItem[slot] = -1;
					while (next < items && (flat.isDone(order[next]) || order[next] > fullItem)) {
						next++;
					}
					if(next < items) {
						slotItem[slot] = order[next++];
						count++;
					}
				}
			}
			if(count == 0) {
				break;
			}
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}
			for (int slot = 0; slot < slots; slot++) {
				int item = slotItem[slot];
				if(item == -1) {
					active[slot] = 0;
				} else {
					// the depth of the slot's item plus one: the children of a split item have longer prefixes than P_DEPTH
					active[slot] = itemDepth[item] + 1;
					System.arraycopy(prefixes, item * n, session.prefixes, slot * n, n);
					System.arraycopy(flat.out(), item * outInts, out, slot * outInts, outInts);
					System.arraycopy(flat.outLongs(), item * FlatSubtree.OUT_LONGS, outLongs, slot * FlatSubtree.OUT_LONGS, FlatSubtree.OUT_LONGS);
				}
			}
			bound = Math.max(bound, tree ? boundVolume : flat.bestVolume());
			longParameters[SubtreeKernel.P_BOUND] = bound;
			flat.best(best);
			longParameters[SubtreeKernel.P_SHARED_VOLUME] = best[0];
			longParameters[SubtreeKernel.P_SHARED_WEIGHT] = best[1];
			longParameters[SubtreeKernel.P_SHARED_COUNT] = best[2];
			// the tree items bound each other by the best so far, which the host carries between the rounds: the first rounds are short, so that a
			// strong arrangement bounds the others before they search much; a tree item keeps its slot, and the items are uneven: when few are
			// left, each runs longer per round, so that the rounds (the transfers and the launch) do not dominate the tail of the search
			int budget = nodeBudget;
			if(tree) {
				budget = (int)Math.min((long)nodeBudget * MAX_BUDGET_FACTOR, (long)nodeBudget * slots / count);
				if(round < 20 && (FIRST_BUDGET << round) < budget) {
					budget = FIRST_BUDGET << round;
				}
			}
			session.parameters[SubtreeKernel.P_BUDGET] = budget;
			round++;

			long startNanos = System.nanoTime();
			session.plan.execute();
			nanosOnDevice += System.nanoTime() - startNanos;
			rounds++;

			boolean overflow = false;
			for (int slot = 0; slot < slots; slot++) {
				int item = slotItem[slot];
				if(item != -1) {
					System.arraycopy(out, slot * outInts, flat.out(), item * outInts, outInts);
					System.arraycopy(outLongs, slot * FlatSubtree.OUT_LONGS, flat.outLongs(), item * FlatSubtree.OUT_LONGS, FlatSubtree.OUT_LONGS);
					if(out[slot * outInts + FlatSubtree.OUT_STATUS] == FlatSubtree.OVERFLOW) {
						overflow = true;
					}
				}
			}
			if(overflow) {
				return null;
			}
		}
		if(anyBest && flat.fullItem() < items) {
			return flat.winner(flat.fullItem());
		}
		return flat.reduce();
	}
}
