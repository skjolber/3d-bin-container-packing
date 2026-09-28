package com.github.skjolber.packing.jmh.boundingbox;

import java.util.List;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.virtualbox.bounds.MultiObjectiveBruteForceVirtualBoxBoundsSearch;
import com.github.skjolber.packing.virtualbox.bounds.SingleObjectiveBruteForceVirtualBoxBoundsSearch;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBounds;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBoundsObjective;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBoundsSearch;

/**
 * Fixed inputs, original identities and objectives reused across invocations.
 * Every factory creates a fresh one-shot search; no prepared or completed search is reused.
 * Thread scope avoids sharing mutable inventory across benchmark workers.
 */
@State(Scope.Thread)
public class BruteForceVirtualBoxBoundsSearchState {

	@Param({"identical", "mixed", "rotated"})
	public String workload;

	@Param({"4", "6"})
	public int boxCount;

	@Param({"false", "true"})
	public boolean load;

	protected List<BoxItem> items;
	protected Container container;
	protected long volume;
	protected VirtualBoxBoundsObjective volumeObjective;
	protected VirtualBoxBoundsObjective filledObjective;
	protected List<VirtualBoxBoundsObjective> singleObjective;
	protected List<VirtualBoxBoundsObjective> volumeAndAxes;
	protected PackagerInterruptSupplier interrupt = () -> false;

	@Setup(Level.Trial)
	public void setup() {
		if(boxCount < 2 || boxCount % 2 != 0) {
			throw new IllegalArgumentException("Expected an even physical box count of at least two");
		}
		switch(workload) {
			case "identical":
				items = List.of(new BoxItem(box("A", 2, 1, false), boxCount, 7, 19));
				break;
			case "mixed":
				items = List.of(new BoxItem(box("A", 2, 1, false), boxCount / 2, 7, 19),
						new BoxItem(box("B", 2, 2, false), boxCount / 2, 11, 23));
				break;
			case "rotated":
				items = List.of(new BoxItem(box("A", 2, 1, true), boxCount, 7, 19));
				break;
			default:
				throw new IllegalArgumentException("Unknown workload: " + workload);
		}
		container = Container.newBuilder().withSize(boxCount, 3, 3).withEmptyWeight(0).withMaxLoadWeight(boxCount).build();
		volume = 0;
		for(BoxItem item : items) {
			volume += item.getBox().getVolume() * item.getCount();
		}
		volumeObjective = new VirtualBoxBoundsObjective("volume", null, VirtualBoxBounds.MIN_VOLUME);
		filledObjective = new VirtualBoxBoundsObjective("volume", bounds -> bounds.getVolume() == volume, VirtualBoxBounds.MIN_VOLUME);
		singleObjective = List.of(volumeObjective);
		volumeAndAxes = List.of(volumeObjective,
				new VirtualBoxBoundsObjective("x", null, VirtualBoxBounds.MIN_X),
				new VirtualBoxBoundsObjective("y", null, VirtualBoxBounds.MIN_Y),
				new VirtualBoxBoundsObjective("z", null, VirtualBoxBounds.MIN_Z));
	}

	protected Box box(String id, int dx, int dy, boolean rotate) {
		var builder = Box.newBuilder().withId(id).withSize(dx, dy, 1).withWeight(1);
		if(rotate) {
			builder.withRotate3D();
		}
		if(load) {
			builder.withMaxLoadWeight(2).withMaxLoadPressure(2).withMaxLoadIdenticalBoxCount(2);
		}
		return builder.build();
	}

	public VirtualBoxBoundsSearch newSingleObjectiveSearch() {
		return new SingleObjectiveBruteForceVirtualBoxBoundsSearch(items, container, volumeObjective, interrupt, load);
	}

	public VirtualBoxBoundsSearch newSingleObjectiveViaMultiSearch() {
		return new MultiObjectiveBruteForceVirtualBoxBoundsSearch(items, container, singleObjective, interrupt, load);
	}

	public VirtualBoxBoundsSearch newVolumeAndAxesSearch() {
		return new MultiObjectiveBruteForceVirtualBoxBoundsSearch(items, container, volumeAndAxes, interrupt, load);
	}

	public VirtualBoxBoundsSearch newFilledGoalSearch() {
		return new SingleObjectiveBruteForceVirtualBoxBoundsSearch(items, container, filledObjective, interrupt, load);
	}
}
