package com.github.skjolber.packing.packer.composite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultComparator;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerStrategyFactory;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.DefaultPackagerResultComparator;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.AbstractPackagerResultBuilder;
import com.github.skjolber.packing.packer.EmptyIntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerInput;

/**
 * Packager which combines other packagers, using costly packagers only where cheaper packagers fall short.
 * <ol>
 * <li>Baseline: the baseline packagers pack the boxes, one after another. The best result is the baseline.</li>
 * <li>Improvement: for each container the container strategy attempts, the packagers are tried in order until one
 * packs all remaining boxes (see {@link CompositePackagerSession}). A costly packager therefore runs only where the
 * cheaper packagers did not pack all remaining boxes, and it only needs to find results which load more than theirs.
 * Without container costs, the improvement uses at most as many containers as the baseline.</li>
 * </ol>
 * The better of the improvement and the baseline is returned. If the improvement is interrupted, the baseline is
 * returned.
 *
 * <pre>
 *  baseline:     plain ────────────────────────────────▶ result A
 *  improvement:  container 1: plain ✔ (all boxes)
 *                container 2: plain ✘ ──▶ brute force ✔
 *                ...                                    ▶ result B
 *  return the better of A and B
 * </pre>
 *
 * Each packager after the first can have a budget: the time it may run for, counted from the start of the
 * improvement. Once the budget is used up, the packager is not used any more.
 * <br>
 * <br>
 * Thread-safe implementation, if the packagers are. Closing this packager closes the packagers.
 */
public class CompositePackager extends AbstractPackager<CompositePackager.CompositePackagerResultBuilder> {

	public static Builder newBuilder() {
		return new Builder();
	}

	/** A packager tried for each container, with a budget in milliseconds or -1 for none. */
	protected static final class Stage {

		protected final AbstractPackager<?> packager;
		protected final long budget;

		protected Stage(AbstractPackager<?> packager, long budget) {
			this.packager = packager;
			this.budget = budget;
		}
	}

	public static class Builder {

		protected final List<AbstractPackager<?>> baselinePackagers = new ArrayList<>();
		protected final List<Stage> stages = new ArrayList<>();
		protected Comparator<IntermediatePackagerResult> comparator;
		protected PackagerResultComparator packagerResultComparator;
		protected ContainerStrategyFactory containerStrategyFactory;

		/**
		 * Add a packager for the baseline. Without baseline packagers, the first packager is used.
		 *
		 * @param packager packager
		 * @return this builder
		 */
		public Builder withBaselinePackager(AbstractPackager<?> packager) {
			this.baselinePackagers.add(Objects.requireNonNull(packager));
			return this;
		}

		/**
		 * Add a packager tried for each container, after the packagers added before it.
		 *
		 * @param packager packager
		 * @return this builder
		 */
		public Builder withPackager(AbstractPackager<?> packager) {
			this.stages.add(new Stage(Objects.requireNonNull(packager), -1L));
			return this;
		}

		/**
		 * Add a packager tried for each container, after the packagers added before it, for a limited time.
		 *
		 * @param packager packager
		 * @param budget the time in milliseconds the packager may run for, counted from the start of the improvement
		 * @return this builder
		 */
		public Builder withPackager(AbstractPackager<?> packager, long budget) {
			if(budget <= 0) {
				throw new IllegalArgumentException("Expected a positive budget, got " + budget);
			}
			this.stages.add(new Stage(Objects.requireNonNull(packager), budget));
			return this;
		}

		/**
		 * @param comparator for selecting the best result for each container
		 * @return this builder
		 */
		public Builder withIntermediatePackagerResultComparator(Comparator<IntermediatePackagerResult> comparator) {
			this.comparator = Objects.requireNonNull(comparator);
			return this;
		}

		/**
		 * @param comparator for selecting the better of the baseline and the improvement
		 * @return this builder
		 */
		public Builder withPackagerResultComparator(PackagerResultComparator comparator) {
			this.packagerResultComparator = Objects.requireNonNull(comparator);
			return this;
		}

		/**
		 * Set the factory which selects the container strategy of the improvement: which containers to use, and in
		 * which order.
		 *
		 * @param factory container strategy factory
		 * @return this builder
		 */
		public Builder withContainerStrategyFactory(ContainerStrategyFactory factory) {
			this.containerStrategyFactory = Objects.requireNonNull(factory);
			return this;
		}

		public CompositePackager build() {
			if(stages.isEmpty()) {
				throw new IllegalStateException("Expected at least one packager");
			}
			if(comparator == null) {
				comparator = new DefaultIntermediatePackagerResultComparator();
			}
			if(packagerResultComparator == null) {
				packagerResultComparator = new DefaultPackagerResultComparator();
			}
			List<AbstractPackager<?>> baseline = baselinePackagers.isEmpty() ? List.of(stages.get(0).packager) : List.copyOf(baselinePackagers);
			CompositePackager packager = new CompositePackager(comparator, packagerResultComparator, baseline, List.copyOf(stages));
			if(containerStrategyFactory != null) {
				packager.setContainerStrategyFactory(containerStrategyFactory);
			}
			return packager;
		}
	}

	public class CompositePackagerResultBuilder extends AbstractPackagerResultBuilder<CompositePackagerResultBuilder> {

		@Override
		public PackagerResult build() {
			return pack(validate(CompositePackager.this), deadline, interrupt);
		}
	}

	protected final PackagerResultComparator packagerResultComparator;
	protected final List<AbstractPackager<?>> baselinePackagers;
	protected final List<Stage> stages;

	protected CompositePackager(Comparator<IntermediatePackagerResult> comparator, PackagerResultComparator packagerResultComparator,
			List<AbstractPackager<?>> baselinePackagers, List<Stage> stages) {
		super(comparator);
		this.packagerResultComparator = packagerResultComparator;
		this.baselinePackagers = baselinePackagers;
		this.stages = stages;
	}

	@Override
	public CompositePackagerResultBuilder newResultBuilder() {
		return new CompositePackagerResultBuilder();
	}

	@Override
	public String getUnsupportedReason(PackagerInput input) {
		for(Stage stage : stages) {
			if(stage.packager.supports(input)) {
				return null;
			}
		}
		return "No packager supports the input: " + stages.get(0).packager.getUnsupportedReason(input);
	}

	@Override
	public PackagerResult pack(PackagerInput input, long deadline, PackagerInterruptSupplier interrupt) {
		long start = System.currentTimeMillis();

		PackagerResult baseline = null;
		for(AbstractPackager<?> packager : baselinePackagers) {
			if(!packager.supports(input)) {
				continue;
			}
			PackagerResult result = packager.pack(input, deadline, interrupt);
			if(result.isTimeout()) {
				// the deadline or interrupt applies to the improvement too
				return withDuration(baseline != null ? baseline : result, start);
			}
			if(baseline == null || packagerResultComparator.compare(result, baseline) > 0) {
				baseline = result;
			}
		}

		PackagerInterruptSupplierBuilder booleanSupplierBuilder = PackagerInterruptSupplierBuilder.builder();
		if(deadline != -1L) {
			booleanSupplierBuilder.withDeadline(deadline);
		}
		if(interrupt != null) {
			booleanSupplierBuilder.withInterrupt(interrupt);
		}
		booleanSupplierBuilder.withScheduledThreadPoolExecutor(scheduledThreadPoolExecutor);

		PackagerInterruptSupplier packagerInterrupt = booleanSupplierBuilder.build();
		CompositePackagerSession session = null;
		try {
			session = newCompositeSession(getImprovementInput(input, baseline), packagerInterrupt);
			ContainerResult result = packSession(packagerInterrupt, session);
			if(result != null) {
				PackagerResult improvement = new PackagerResult(result.getPackList(), 0L, false, result.getCost());
				if(baseline == null || packagerResultComparator.compare(improvement, baseline) > 0) {
					return withDuration(improvement, start);
				}
			}
		} catch (PackagerInterruptedException e) {
			if(baseline == null) {
				return new PackagerResult(Collections.emptyList(), System.currentTimeMillis() - start, true, -1);
			}
		} finally {
			packagerInterrupt.close();
			if(session != null) {
				session.close();
			}
		}
		if(baseline == null) {
			return new PackagerResult(Collections.emptyList(), System.currentTimeMillis() - start, false, -1);
		}
		return withDuration(baseline, start);
	}

	/**
	 * Without container costs, more containers than the baseline are never better.
	 */
	protected PackagerInput getImprovementInput(PackagerInput input, PackagerResult baseline) {
		if(baseline == null || !baseline.isSuccess() || baseline.size() >= input.getMaxContainerCount()) {
			return input;
		}
		for(ContainerItem containerItem : input.getContainerItems()) {
			if(containerItem.hasCostCalculator()) {
				return input;
			}
		}
		return new PackagerInput(input.getBoxItems(), input.getBoxItemGroups(), input.getContainerItems(), baseline.size(), input.getOrder());
	}

	private static PackagerResult withDuration(PackagerResult result, long start) {
		return new PackagerResult(result.getContainers(), System.currentTimeMillis() - start, result.isTimeout(), result.getCost());
	}

	@Override
	protected PackagerSession newSession(PackagerInput input, PackagerInterruptSupplier interrupt) {
		return newCompositeSession(input, interrupt);
	}

	/**
	 * Create a session with a session for each packager which supports the input. The packagers' sessions have
	 * their own copies of the container items.
	 */
	protected CompositePackagerSession newCompositeSession(PackagerInput input, PackagerInterruptSupplier interrupt) {
		long start = System.currentTimeMillis();

		List<PackagerSession> sessions = new ArrayList<>(stages.size());
		List<PackagerInterruptSupplier> stageInterrupts = new ArrayList<>(stages.size());
		for(Stage stage : stages) {
			if(!stage.packager.supports(input)) {
				continue;
			}
			PackagerInterruptSupplier stageInterrupt = interrupt;
			if(!sessions.isEmpty() && stage.budget != -1L) {
				// the first packager bounds the others, so it has no budget
				stageInterrupt = PackagerInterruptSupplierBuilder.builder()
						.withInterrupt(interrupt)
						.withDeadline(start + stage.budget)
						.withScheduledThreadPoolExecutor(scheduledThreadPoolExecutor)
						.build();
				stageInterrupts.add(stageInterrupt);
			}
			sessions.add(stage.packager.createSession(input, stageInterrupt));
		}
		boolean hints = prefersHigherLoadVolume;
		return new CompositePackagerSession(sessions.toArray(new PackagerSession[sessions.size()]), interrupt, stageInterrupts, intermediatePackagerResultComparator, hints,
				createEmptyIntermediatePackagerResult());
	}

	@Override
	protected IntermediatePackagerResult createEmptyIntermediatePackagerResult() {
		return EmptyIntermediatePackagerResult.EMPTY;
	}

	@Override
	public void close() {
		super.close();
		for(AbstractPackager<?> packager : baselinePackagers) {
			packager.close();
		}
		for(Stage stage : stages) {
			stage.packager.close();
		}
	}
}
