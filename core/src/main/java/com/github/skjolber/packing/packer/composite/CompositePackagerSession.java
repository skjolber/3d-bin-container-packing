package com.github.skjolber.packing.packer.composite;

import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerInventory;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResult;

/**
 * Session of several packagers for the same packaging operation, cheapest first. Each attempt tries the
 * packagers in order, until one packs all remaining boxes into the container:
 *
 * <pre>
 *  attempt(container):  packager 0 ──▶ all boxes? ── yes ─▶ result
 *                                         │ no
 *                       packager 1 ──▶ all boxes? ── yes ─▶ result
 *                                         │ no
 *                       ...           the best result
 * </pre>
 *
 * A packager gets the best result of the cheaper packagers as a hint, so that it can skip searches which
 * cannot load more. Accepted results are accepted by every packager's session, keeping them in sync.
 * <p>
 * Each packager's session has its own interrupt. Except for the first packager, an interrupt which is not
 * the session's interrupt means the packager's budget is used up: the packager is not used any more. Likewise, a
 * packager whose session cannot accept a result (for example box item groups packed out of order) is not used any
 * more. The first packager still in use answers the questions about the remaining boxes and containers.
 */
public class CompositePackagerSession implements PackagerSession {

	protected final PackagerSession[] sessions;
	protected final boolean[] active;
	protected final PackagerInterruptSupplier interrupt;
	protected final List<PackagerInterruptSupplier> stageInterrupts;
	protected final Comparator<IntermediatePackagerResult> comparator;
	protected final boolean hints;
	protected final IntermediatePackagerResult emptyResult;

	/**
	 * @param sessions sessions for the same packaging operation, cheapest first
	 * @param interrupt the interrupt of the packaging operation
	 * @param stageInterrupts the packagers' own interrupts, closed by {@link #close()}
	 * @param comparator for selecting the best result
	 * @param hints whether results with less load volume always compare worse, so that the best result can be passed on as a hint
	 * @param emptyResult result when no packager packs any box
	 */
	public CompositePackagerSession(PackagerSession[] sessions, PackagerInterruptSupplier interrupt, List<PackagerInterruptSupplier> stageInterrupts,
			Comparator<IntermediatePackagerResult> comparator, boolean hints, IntermediatePackagerResult emptyResult) {
		this(sessions, newActive(sessions), interrupt, stageInterrupts, comparator, hints, emptyResult);
	}

	protected CompositePackagerSession(PackagerSession[] sessions, boolean[] active, PackagerInterruptSupplier interrupt, List<PackagerInterruptSupplier> stageInterrupts,
			Comparator<IntermediatePackagerResult> comparator, boolean hints, IntermediatePackagerResult emptyResult) {
		this.sessions = sessions;
		this.active = active;
		this.interrupt = interrupt;
		this.stageInterrupts = stageInterrupts;
		this.comparator = comparator;
		this.hints = hints;
		this.emptyResult = emptyResult;
	}

	private static boolean[] newActive(PackagerSession[] sessions) {
		boolean[] active = new boolean[sessions.length];
		for(int i = 0; i < active.length; i++) {
			active[i] = true;
		}
		return active;
	}

	@Override
	public IntermediatePackagerResult attempt(int containerIndex, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
		int remainingBoxes = primary().countRemainingBoxes();

		IntermediatePackagerResult selected = null;
		int selectedStage = -1;
		for(int i = 0; i < sessions.length; i++) {
			if(!active[i]) {
				continue;
			}
			IntermediatePackagerResult result;
			try {
				result = sessions[i].attempt(containerIndex, hints ? getHint(best, selected) : null, abortOnAnyBoxTooBig);
			} catch(PackagerInterruptedException e) {
				if(i == 0 || i == getPrimaryIndex() || interrupt.getAsBoolean()) {
					throw e;
				}
				// the packager's budget is used up; its session is not used any more
				active[i] = false;
				continue;
			}
			if(result != null && !result.isEmpty() && (selected == null || comparator.compare(selected, result) < 0)) {
				selected = result;
				selectedStage = i;
			}
			if(selected != null && selected.getStack().size() == remainingBoxes) {
				// no packager can do better for this container
				break;
			}
		}
		if(selected == null) {
			return emptyResult;
		}
		return new CompositeIntermediatePackagerResult(selected, selectedStage);
	}

	private static IntermediatePackagerResult getHint(IntermediatePackagerResult best, IntermediatePackagerResult selected) {
		if(best == null || best.isEmpty()) {
			return selected;
		}
		if(selected == null || selected.getLoadVolume() < best.getLoadVolume()) {
			return best;
		}
		return selected;
	}

	@Override
	public IntermediatePackagerResult peek(int containerIndex, IntermediatePackagerResult existing) {
		IntermediatePackagerResult result;
		int stage;
		if(existing instanceof CompositeIntermediatePackagerResult compositeResult && active[compositeResult.getStage()]) {
			stage = compositeResult.getStage();
			result = sessions[stage].peek(containerIndex, compositeResult.getDelegate());
		} else {
			stage = getPrimaryIndex();
			result = sessions[stage].peek(containerIndex, existing);
		}
		if(result == null) {
			return null;
		}
		return new CompositeIntermediatePackagerResult(result, stage);
	}

	@Override
	public Container accept(IntermediatePackagerResult result) {
		int owner = 0;
		IntermediatePackagerResult delegate = result;
		if(result instanceof CompositeIntermediatePackagerResult compositeResult) {
			owner = compositeResult.getStage();
			delegate = compositeResult.getDelegate();
		}
		Container container;
		if(active[owner]) {
			container = sessions[owner].accept(delegate);
		} else {
			// the result is still valid, but its session is not used any more
			owner = getPrimaryIndex();
			container = sessions[owner].accept(new DefaultIntermediatePackagerResult(delegate.getContainerItem(), delegate.getStack()));
		}
		// the other sessions remove the same boxes and container
		IntermediatePackagerResult accepted = new DefaultIntermediatePackagerResult(delegate.getContainerItem(), container.getStack());
		for(int i = 0; i < sessions.length; i++) {
			if(i != owner && active[i]) {
				try {
					sessions[i].accept(accepted);
				} catch(IllegalArgumentException e) {
					// the packager cannot continue from this result
					active[i] = false;
				}
			}
		}
		return container;
	}

	@Override
	public List<Integer> getContainers() {
		return primary().getContainers();
	}

	@Override
	public PackagerSession fresh() {
		PackagerSession[] fresh = new PackagerSession[sessions.length];
		for(int i = 0; i < sessions.length; i++) {
			if(active[i]) {
				fresh[i] = sessions[i].fresh();
			}
		}
		return new CompositePackagerSession(fresh, active.clone(), interrupt, stageInterrupts, comparator, hints, emptyResult);
	}

	@Override
	public PackagerSession fork() {
		PackagerSession[] forks = new PackagerSession[sessions.length];
		for(int i = 0; i < sessions.length; i++) {
			if(active[i]) {
				forks[i] = sessions[i].fork();
			}
		}
		return new CompositePackagerSession(forks, active.clone(), interrupt, stageInterrupts, comparator, hints, emptyResult);
	}

	@Override
	public long getRemainingVolume() {
		return primary().getRemainingVolume();
	}

	@Override
	public long getRemainingWeight() {
		return primary().getRemainingWeight();
	}

	@Override
	public ContainerInventory getContainerInventory() {
		return primary().getContainerInventory();
	}

	@Override
	public List<BoxItem> getRemainingBoxItems() {
		return primary().getRemainingBoxItems();
	}

	@Override
	public List<BoxItemGroup> getRemainingBoxItemGroups() {
		return primary().getRemainingBoxItemGroups();
	}

	@Override
	public ContainerItem getContainerItem(int index) {
		return primary().getContainerItem(index);
	}

	@Override
	public int countRemainingBoxes() {
		return primary().countRemainingBoxes();
	}

	@Override
	public int countRemainingBoxItemGroups() {
		return primary().countRemainingBoxItemGroups();
	}

	@Override
	public int getMaxContainerCount() {
		return primary().getMaxContainerCount();
	}

	/** @return the index of the first packager still in use, or 0 if none */
	protected int getPrimaryIndex() {
		for(int i = 0; i < active.length; i++) {
			if(active[i]) {
				return i;
			}
		}
		return 0;
	}

	protected PackagerSession primary() {
		return sessions[getPrimaryIndex()];
	}

	/** @return whether the packager is still used, i.e. its budget is not used up */
	public boolean isActive(int stage) {
		return active[stage];
	}

	/** Close the packagers' own interrupts. */
	public void close() {
		for(PackagerInterruptSupplier stageInterrupt : stageInterrupts) {
			stageInterrupt.close();
		}
		for(PackagerSession session : sessions) {
			if(session instanceof CompositePackagerSession compositeSession) {
				compositeSession.close();
			}
		}
	}
}
