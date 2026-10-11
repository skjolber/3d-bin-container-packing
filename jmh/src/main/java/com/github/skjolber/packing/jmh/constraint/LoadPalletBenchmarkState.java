package com.github.skjolber.packing.jmh.constraint;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * JMH state for packing a pallet of 48 mixed boxes (8 types × 6, all rotatable) with and without
 * load limits, so that the cost of the load calculations can be compared to the same packing
 * without them.
 * <ul>
 *   <li>{@code none}: no load limits</li>
 *   <li>{@code weight}: max load weight (generous, never binding)</li>
 *   <li>{@code wpc}: max load weight, pressure and box count (generous, never binding)</li>
 * </ul>
 */
@State(Scope.Benchmark)
public class LoadPalletBenchmarkState {

	private static final int[][] TYPES = {
			// dx, dy, dz, weight
			{ 400, 300, 200, 12 },
			{ 300, 200, 150, 6 },
			{ 600, 400, 300, 20 },
			{ 200, 200, 200, 4 },
			{ 400, 400, 250, 15 },
			{ 300, 300, 300, 9 },
			{ 500, 300, 100, 7 },
			{ 250, 150, 120, 3 } };

	private static final int COPIES = 6;

	@Param({ "none", "weight", "wpc" })
	private String limits = "wpc";

	private PlainPackager plainPackager;
	private LargestAreaFitFirstPackager laffPackager;
	private List<ContainerItem> containers;
	private List<BoxItem> items;

	@Setup(Level.Trial)
	public void init() {
		plainPackager = PlainPackager.newBuilder().build();
		laffPackager = LargestAreaFitFirstPackager.newBuilder().build();
		containers = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("pallet")
						.withEmptyWeight(25)
						.withSize(1200, 800, 1600)
						.withMaxLoadWeight(10_000_000)
						.build(), 1)
				.build();
		items = createItems(limits);
	}

	public static List<BoxItem> createItems(String limits) {
		List<BoxItem> items = new ArrayList<>();
		for(int[] type : TYPES) {
			Box.Builder builder = Box.newBuilder()
					.withSize(type[0], type[1], type[2])
					.withWeight(type[3])
					.withRotate3D();
			switch (limits) {
			case "weight":
				builder.withMaxLoadWeight(100_000);
				break;
			case "wpc":
				builder.withMaxLoadWeight(100_000)
						.withMaxLoadPressure(1000.0)
						.withMaxLoadBoxCount(1000);
				break;
			default:
			}
			items.add(new BoxItem(builder.build(), COPIES));
		}
		return items;
	}

	@TearDown(Level.Trial)
	public void shutdown() {
		plainPackager.close();
		laffPackager.close();
	}

	public PlainPackager getPlainPackager() {
		return plainPackager;
	}

	public LargestAreaFitFirstPackager getLaffPackager() {
		return laffPackager;
	}

	public List<ContainerItem> getContainers() {
		return containers;
	}

	public List<BoxItem> getItems() {
		return items;
	}
}
