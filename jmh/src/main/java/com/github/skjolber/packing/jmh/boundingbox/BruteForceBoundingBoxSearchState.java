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
import com.github.skjolber.packing.boundingbox.BoundingBox;
import com.github.skjolber.packing.boundingbox.BoundingBoxObjective;
import com.github.skjolber.packing.boundingbox.BruteForceBoundingBoxSearch;
import com.github.skjolber.packing.boundingbox.MultiObjectiveBruteForceBoundingBoxSearch;
import com.github.skjolber.packing.boundingbox.SingleObjectiveBruteForceBoundingBoxSearch;

/**
 * Fixed inputs, original identities and objectives reused across invocations.
 * Every factory creates a fresh one-shot search; no prepared or completed search is reused.
 * Thread scope avoids sharing mutable inventory across benchmark workers.
 */
@State(Scope.Thread)
public class BruteForceBoundingBoxSearchState {

	@Param({"identical", "mixed", "rotated"})
	public String workload;

	@Param({"4", "6"})
	public int boxCount;

	@Param({"false", "true"})
	public boolean load;

	protected List<BoxItem> items;
	protected Container container;
	protected long volume;
	protected BoundingBoxObjective volumeObjective;
	protected BoundingBoxObjective filledObjective;
	protected List<BoundingBoxObjective> singleObjective;
	protected List<BoundingBoxObjective> volumeAndAxes;
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
		volumeObjective = new BoundingBoxObjective("volume", null, BoundingBox.MIN_VOLUME);
		filledObjective = new BoundingBoxObjective("volume", bounds -> bounds.getVolume() == volume, BoundingBox.MIN_VOLUME);
		singleObjective = List.of(volumeObjective);
		volumeAndAxes = List.of(volumeObjective,
				new BoundingBoxObjective("x", null, BoundingBox.MIN_X),
				new BoundingBoxObjective("y", null, BoundingBox.MIN_Y),
				new BoundingBoxObjective("z", null, BoundingBox.MIN_Z));
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

	public BruteForceBoundingBoxSearch newSingleObjectiveSearch() {
		return new SingleObjectiveBruteForceBoundingBoxSearch(items, container, volumeObjective, interrupt, load);
	}

	public BruteForceBoundingBoxSearch newSingleObjectiveViaMultiSearch() {
		return new MultiObjectiveBruteForceBoundingBoxSearch(items, container, singleObjective, interrupt, load);
	}

	public BruteForceBoundingBoxSearch newVolumeAndAxesSearch() {
		return new MultiObjectiveBruteForceBoundingBoxSearch(items, container, volumeAndAxes, interrupt, load);
	}

	public BruteForceBoundingBoxSearch newFilledGoalSearch() {
		return new SingleObjectiveBruteForceBoundingBoxSearch(items, container, filledObjective, interrupt, load);
	}
}
