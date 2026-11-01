package com.github.skjolber.packing.tornado;

import java.util.List;

import com.github.skjolber.packing.accelerator.FlatLayout;

import uk.ac.manchester.tornado.api.ImmutableTaskGraph;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.common.TornadoDevice;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;

/**
 * Run by hand, like {@linkplain GpuSmoke}: which kind of kernel parameters does the runtime take: plain arrays, and an object with final int fields (the layout).
 */
public class ArrayProbe {

	public static void plainArrays(int[] a, long[] b, int[] out, int n) {
		for (@Parallel int i = 0; i < n; i++) {
			out[i] = a[i] + (int)b[i];
		}
	}

	public static void withLayout(FlatLayout layout, int[] s, int[] out, int n) {
		for (@Parallel int i = 0; i < n; i++) {
			int base = i * layout.size;
			layout.reset(s, base);
			out[i] = s[base + layout.headX + 3] + s[base + layout.flags] + layout.size;
		}
	}

	public static void main(String[] args) throws Exception {
		List<DeviceInfo> devices = TornadoDevices.discover();
		DeviceInfo info = DeviceSelector.newBuilder().build().select(devices).orElseThrow(() -> new IllegalStateException("No device found: " + devices));
		System.out.println("Device: " + info);
		TornadoDevice device = TornadoExecutionPlan.getDevice(info.backendIndex(), info.deviceIndex());

		String which = args.length > 0 ? args[0] : "all";
		if(!which.equals("layout")) {
			plain(device);
		}
		if(!which.equals("plain")) {
			layout(device);
		}
	}

	private static void plain(TornadoDevice device) throws Exception {
		int n = 1024;
		int[] a = new int[n];
		long[] b = new long[n];
		int[] out = new int[n];
		for (int i = 0; i < n; i++) {
			a[i] = i;
			b[i] = 3L * i;
		}
		TaskGraph graph = new TaskGraph("plain").transferToDevice(DataTransferMode.EVERY_EXECUTION, a, b).task("t", ArrayProbe::plainArrays, a, b, out, n)
				.transferToHost(DataTransferMode.EVERY_EXECUTION, out);
		ImmutableTaskGraph immutable = graph.snapshot();
		try (TornadoExecutionPlan plan = new TornadoExecutionPlan(immutable)) {
			plan.withDevice(device);
			plan.execute();
			for (int i = 0; i < n; i++) {
				if(out[i] != 4 * i) {
					throw new IllegalStateException("plain: wrong value at " + i + ": " + out[i]);
				}
			}
			System.out.println("plain int[]/long[] parameters OK");
		}
	}

	private static void layout(TornadoDevice device) throws Exception {
		int n = 256;
		FlatLayout layout = new FlatLayout(8, 16);
		int[] s = new int[n * layout.size];
		int[] out = new int[n];
		TaskGraph graph = new TaskGraph("layout").transferToDevice(DataTransferMode.EVERY_EXECUTION, layout, s).task("t", ArrayProbe::withLayout, layout, s, out, n)
				.transferToHost(DataTransferMode.EVERY_EXECUTION, out);
		ImmutableTaskGraph immutable = graph.snapshot();
		try (TornadoExecutionPlan plan = new TornadoExecutionPlan(immutable)) {
			plan.withDevice(device);
			plan.execute();
			for (int i = 0; i < n; i++) {
				if(out[i] != -1 + layout.size) {
					throw new IllegalStateException("layout: wrong value at " + i + ": " + out[i]);
				}
			}
			System.out.println("object parameter (FlatLayout) OK");
		}
	}
}
