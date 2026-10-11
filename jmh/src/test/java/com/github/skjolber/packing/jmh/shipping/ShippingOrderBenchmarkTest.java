package com.github.skjolber.packing.jmh.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * Current results for the shipping orders from GitHub issue #1158. Brute force finds the best
 * packing in both cases; the greedy packagers do not always (one box at a time).
 */
public class ShippingOrderBenchmarkTest {

	/** The JSON order data is read with all containers, counts and rotations. */
	@Test
	public void readsOrderFromJson() {
		List<ContainerItem> containers = ShippingOrders.getContainers();
		assertThat(containers).hasSize(7);
		assertThat(containers.get(2).getContainer().getId()).isEqualTo("CURBY2SL");
		assertThat(containers.get(2).getContainer().getMaxLoadWeight()).isEqualTo(499200);

		List<BoxItem> order = ShippingOrders.getOrder();
		assertThat(order).hasSize(2);
		assertThat(order.get(0).getCount()).isEqualTo(4);
		assertThat(order.get(0).getBox().getId()).isEqualTo("HIP1-CH-HG");
		assertThat(order.get(0).getBox().getStackValues()).hasSize(6);
		assertThat(order.get(1).getCount()).isEqualTo(1);
		assertThat(order.get(1).getBox().getWeight()).isEqualTo(1600);
	}

	@Test
	public void bestOfPacksOrderInSmallestFittingContainer() {
		ShippingOrderBenchmarkState state = new ShippingOrderBenchmarkState();
		state.init();
		try {
			PackagerResult result = new ShippingOrderBenchmark().bestOf(state);

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.size()).isEqualTo(1);
			assertThat(result.get(0).getId()).isEqualTo("CURBY2SL");
			assertThat(result.get(0).getStack().size()).isEqualTo(5);
		} finally {
			state.shutdown();
		}
	}

	@Test
	public void compositePacksOrderInSmallestFittingContainer() {
		ShippingOrderBenchmarkState state = new ShippingOrderBenchmarkState();
		state.init();
		try {
			PackagerResult result = new ShippingOrderBenchmark().compositePackager(state);
			assertThat(result.size()).isEqualTo(1);
			assertThat(result.get(0).getId()).isEqualTo("CURBY2SL");
			assertThat(result.get(0).getStack().size()).isEqualTo(5);
		} finally {
			state.shutdown();
		}
	}

	@Test
	public void bruteForceAndPlainFindSmallestContainer() {
		try (BruteForcePackager bruteForce = BruteForcePackager.newBuilder().build(); PlainPackager plain = PlainPackager.newBuilder().build()) {
			assertThat(ShippingOrderBenchmark.pack(bruteForce, ShippingOrders.getContainers(), ShippingOrders.getOrder()).get(0).getId()).isEqualTo("CURBY2SL");
			assertThat(ShippingOrderBenchmark.pack(plain, ShippingOrders.getContainers(), ShippingOrders.getOrder()).get(0).getId()).isEqualTo("CURBY2SL");
		}
	}

	/** Known limitation: LAFF builds levels, and cannot mix the thick and flat items in one level. */
	@Test
	public void laffUsesLargerContainer() {
		try (LargestAreaFitFirstPackager laff = LargestAreaFitFirstPackager.newBuilder().build()) {
			PackagerResult result = ShippingOrderBenchmark.pack(laff, ShippingOrders.getContainers(), ShippingOrders.getOrder());

			assertThat(result.size()).isEqualTo(1);
			assertThat(result.get(0).getId()).isEqualTo("CURBY5");
		}
	}

	/**
	 * <pre>
	 *  top view of the 21375 x 9750 container
	 *  +-------+-------+-------+
	 *  |   A   |   A   |   A   |  9500 deep, 7125 wide each
	 *  +-------+-------+-------+
	 * </pre>
	 */
	@Test
	public void bruteForcePacksRowInOneContainer() {
		try (BruteForcePackager bruteForce = BruteForcePackager.newBuilder().build()) {
			PackagerResult result = ShippingOrderBenchmark.pack(bruteForce, ShippingOrders.getRowContainer(), ShippingOrders.getRowBoxes());

			assertThat(result.size()).isEqualTo(1);
			assertThat(result.get(0).getStack().size()).isEqualTo(3);
		}
	}

	/** Known limitation: equal-area rotations tie, and the first one only fits two boxes in a row. */
	@Test
	public void greedyPackagersUseTwoContainersForRow() {
		try (LargestAreaFitFirstPackager laff = LargestAreaFitFirstPackager.newBuilder().build(); PlainPackager plain = PlainPackager.newBuilder().build()) {
			assertThat(ShippingOrderBenchmark.pack(laff, ShippingOrders.getRowContainer(), ShippingOrders.getRowBoxes()).size()).isEqualTo(2);
			assertThat(ShippingOrderBenchmark.pack(plain, ShippingOrders.getRowContainer(), ShippingOrders.getRowBoxes()).size()).isEqualTo(2);
		}
	}
}
