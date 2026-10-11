package com.github.skjolber.packing.jmh.composite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.comparator.DefaultPackagerResultComparator;
import com.github.skjolber.packing.jmh.shipping.ShippingOrders;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.composite.CompositePackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * Quality and time of the packagers, and of composite packagers, for random orders in the shipping containers of
 * GitHub issue #1158 (seven container sizes, smallest first). For each order size, every packager packs the same
 * seeded orders; reported are the orders packed, the containers used, their total volume (a proxy for shipping cost)
 * and the time per order.
 * <p>
 * Run with {@code java -cp jmh/target/benchmark.jar com.github.skjolber.packing.jmh.composite.CompositeQualityReport [orders] [deadline ms]}.
 */
public class CompositeQualityReport {

	private static final int MAX_CONTAINERS = 5;

	/** The result of one packager for a set of orders. */
	public static class Summary {

		protected int packed;
		protected int containers;
		protected long containerVolume;
		protected long totalMillis;
		protected long maxMillis;
		protected int worseThanBest;

		protected void add(PackagerResult result, long millis) {
			if(result.isSuccess()) {
				packed++;
				containers += result.size();
				for(int i = 0; i < result.size(); i++) {
					containerVolume += result.get(i).getVolume();
				}
			}
			totalMillis += millis;
			maxMillis = Math.max(maxMillis, millis);
		}

		public int getPacked() {
			return packed;
		}

		public int getWorseThanBest() {
			return worseThanBest;
		}
	}

	public static void main(String[] args) {
		int orders = args.length > 0 ? Integer.parseInt(args[0]) : 30;
		long deadline = args.length > 1 ? Long.parseLong(args[1]) : 1000;

		for(int itemTypes : new int[] {3, 6, 10}) {
			Map<String, Summary> summaries = run(itemTypes, orders, deadline);
			print(itemTypes, orders, deadline, summaries);
		}
	}

	/**
	 * @param itemTypes box types per order (each with a count of 1 to 3)
	 * @param orders number of orders
	 * @param deadline time limit per order in milliseconds, for the brute-force packagers; the composites give brute
	 *        force a fifth of it
	 * @return summaries by packager, in a fixed order
	 */
	public static Map<String, Summary> run(int itemTypes, int orders, long deadline) {
		List<ContainerItem> containers = ShippingOrders.getContainers();
		List<List<BoxItem>> orderList = new ArrayList<>();
		Random random = new Random(itemTypes * 1000L + orders);
		for(int i = 0; i < orders; i++) {
			orderList.add(createOrder(random, itemTypes));
		}

		Map<String, Supplier<AbstractPackager<?>>> packagers = new LinkedHashMap<>();
		packagers.put("plain", () -> PlainPackager.newBuilder().build());
		packagers.put("laff", () -> LargestAreaFitFirstPackager.newBuilder().build());
		packagers.put("fast brute force", () -> FastBruteForcePackager.newBuilder().build());
		packagers.put("brute force", () -> BruteForcePackager.newBuilder().build());
		packagers.put("composite: plain, fast brute force", () -> CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(FastBruteForcePackager.newBuilder().build(), deadline / 5)
				.build());
		packagers.put("composite: plain, brute force", () -> CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(BruteForcePackager.newBuilder().build(), deadline / 5)
				.build());

		DefaultPackagerResultComparator comparator = new DefaultPackagerResultComparator();
		Map<String, Summary> summaries = new LinkedHashMap<>();
		Map<String, List<PackagerResult>> results = new LinkedHashMap<>();
		for(Map.Entry<String, Supplier<AbstractPackager<?>>> entry : packagers.entrySet()) {
			Summary summary = new Summary();
			List<PackagerResult> packagerResults = new ArrayList<>();
			try (AbstractPackager<?> packager = entry.getValue().get()) {
				// warm up
				for(int i = 0; i < Math.min(5, orders); i++) {
					pack(packager, containers, orderList.get(i), deadline);
				}
				for(List<BoxItem> order : orderList) {
					long start = System.nanoTime();
					PackagerResult result = pack(packager, containers, order, deadline);
					summary.add(result, (System.nanoTime() - start) / 1_000_000);
					packagerResults.add(result);
				}
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
			summaries.put(entry.getKey(), summary);
			results.put(entry.getKey(), packagerResults);
		}

		// the best result per order, over all packagers
		for(int i = 0; i < orders; i++) {
			PackagerResult best = null;
			for(List<PackagerResult> packagerResults : results.values()) {
				PackagerResult result = packagerResults.get(i);
				if(best == null || comparator.compare(result, best) > 0) {
					best = result;
				}
			}
			for(Map.Entry<String, List<PackagerResult>> entry : results.entrySet()) {
				if(comparator.compare(best, entry.getValue().get(i)) > 0) {
					summaries.get(entry.getKey()).worseThanBest++;
				}
			}
		}
		return summaries;
	}

	private static PackagerResult pack(AbstractPackager<?> packager, List<ContainerItem> containers, List<BoxItem> order, long deadline) {
		return packager.newResultBuilder()
				.withContainerItems(containers)
				.withBoxItems(order)
				.withMaxContainerCount(MAX_CONTAINERS)
				.withInterruptDuration(deadline)
				.build();
	}

	/** Boxes from 0.5 to 6 inches (in 1/10000 inch, in steps of 1/4 inch, so that some sides are equal). */
	private static List<BoxItem> createOrder(Random random, int itemTypes) {
		List<BoxItem> order = new ArrayList<>();
		for(int i = 0; i < itemTypes; i++) {
			Box box = Box.newBuilder()
					.withId("box-" + i)
					.withSize(side(random), side(random), side(random))
					.withRotate3D()
					.withWeight(100 + random.nextInt(20_000))
					.build();
			order.add(new BoxItem(box, 1 + random.nextInt(3)));
		}
		return order;
	}

	private static int side(Random random) {
		return 5_000 + 2_500 * random.nextInt(23);
	}

	private static void print(int itemTypes, int orders, long deadline, Map<String, Summary> summaries) {
		System.out.println();
		System.out.println(String.format(Locale.ROOT, "%d orders of %d box types (1-3 each), time limit %d ms", orders, itemTypes, deadline));
		System.out.println(String.format(Locale.ROOT, "%-36s %7s %10s %16s %12s %8s %11s", "packager", "packed", "containers", "volume (in^3)", "mean ms", "max ms", "not best"));
		for(Map.Entry<String, Summary> entry : summaries.entrySet()) {
			Summary s = entry.getValue();
			System.out.println(String.format(Locale.ROOT, "%-36s %7d %10d %16.0f %12.1f %8d %11d", entry.getKey(), s.packed, s.containers,
					s.containerVolume / 1e12, s.totalMillis / (double)orders, s.maxMillis, s.worseThanBest));
		}
	}
}
