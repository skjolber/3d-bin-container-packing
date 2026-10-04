package com.github.skjolber.packing.jmh.shipping;

import java.util.List;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

@State(Scope.Benchmark)
public class ShippingOrderBenchmarkState {

	private BruteForcePackager bruteForcePackager;
	private LargestAreaFitFirstPackager laffPackager;
	private PlainPackager plainPackager;

	private List<ContainerItem> containers;
	private List<BoxItem> order;

	@Setup(Level.Trial)
	public void init() {
		bruteForcePackager = BruteForcePackager.newBuilder().build();
		laffPackager = LargestAreaFitFirstPackager.newBuilder().build();
		plainPackager = PlainPackager.newBuilder().build();
		containers = ShippingOrders.getContainers();
		order = ShippingOrders.getOrder();
	}

	@TearDown(Level.Trial)
	public void shutdown() {
		bruteForcePackager.close();
		laffPackager.close();
		plainPackager.close();
	}

	public BruteForcePackager getBruteForcePackager() {
		return bruteForcePackager;
	}

	public LargestAreaFitFirstPackager getLaffPackager() {
		return laffPackager;
	}

	public PlainPackager getPlainPackager() {
		return plainPackager;
	}

	public List<ContainerItem> getContainers() {
		return containers;
	}

	public List<BoxItem> getOrder() {
		return order;
	}
}
