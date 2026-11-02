package com.github.skjolber.packing.tornado;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import uk.ac.manchester.tornado.api.AccessorParameters;
import uk.ac.manchester.tornado.api.GridScheduler;
import uk.ac.manchester.tornado.api.ImmutableTaskGraph;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.WorkerGrid1D;
import uk.ac.manchester.tornado.api.common.Access;
import uk.ac.manchester.tornado.api.common.TornadoDevice;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;

/**
 * Run by hand, like {@linkplain GpuSmoke}: checks the calling convention of a prebuilt OpenCL kernel in this TornadoVM version: the order of the
 * parameters, the offset of the data of an {@code int[]} and a {@code long[]} from the start of their device buffers, and the global work size.
 */
public class PrebuiltProbe {

	private static final String KERNEL = """
			__kernel void probe(__global long *_kernel_context, __constant uchar *_constant_region, __local uchar *_local_region, __global int *_atomics,
					__global uchar *aBuffer, __global uchar *bBuffer, __global uchar *outBuffer, __global uchar *sizeBuffer) {
				int i = get_global_id(0);
				__global int *a = (__global int *) (aBuffer + HEADER);
				__global long *b = (__global long *) (bBuffer + HEADER);
				__global long *out = (__global long *) (outBuffer + HEADER);
				__global int *size = (__global int *) (sizeBuffer + HEADER);
				if (i == 0) {
					size[0] = get_global_size(0);
					size[1] = (int) (*((__global int *) aBuffer));
					size[2] = (int) (*((__global int *) (aBuffer + 4)));
					size[3] = (int) (*((__global int *) (aBuffer + 8)));
					size[4] = (int) (*((__global int *) (aBuffer + 12)));
				}
				out[i] = a[i] + b[i];
			}
			""";

	public static void main(String[] args) throws Exception {
		int header = args.length > 0 ? Integer.parseInt(args[0]) : 16;
		List<DeviceInfo> devices = TornadoDevices.discover();
		DeviceInfo info = DeviceSelector.newBuilder().build().select(devices).orElseThrow(() -> new IllegalStateException("No device found: " + devices));
		System.out.println("Device: " + info);
		TornadoDevice device = TornadoExecutionPlan.getDevice(info.backendIndex(), info.deviceIndex());

		int n = 1000;
		int[] a = new int[n];
		long[] b = new long[n];
		long[] out = new long[n];
		int[] size = new int[8];
		for (int i = 0; i < n; i++) {
			a[i] = i;
			b[i] = 3L * i + 1;
		}
		Path file = write(KERNEL.replace("HEADER", Integer.toString(header)));

		AccessorParameters accessors = new AccessorParameters(4);
		accessors.set(0, a, Access.READ_ONLY);
		accessors.set(1, b, Access.READ_ONLY);
		accessors.set(2, out, Access.WRITE_ONLY);
		accessors.set(3, size, Access.WRITE_ONLY);
		TaskGraph graph = new TaskGraph("probe").transferToDevice(DataTransferMode.FIRST_EXECUTION, a, b).prebuiltTask("t", "probe", file.toString(), accessors)
				.transferToHost(DataTransferMode.EVERY_EXECUTION, out, size);
		ImmutableTaskGraph immutable = graph.snapshot();
		GridScheduler scheduler = new GridScheduler("probe.t", new WorkerGrid1D(n));
		try (TornadoExecutionPlan plan = new TornadoExecutionPlan(immutable)) {
			plan.withDevice(device).withGridScheduler(scheduler);
			long start = System.nanoTime();
			plan.execute();
			System.out.println("first run " + (System.nanoTime() - start) / 1_000_000 + " ms, global size " + size[0] + ", header words " + size[1] + " " + size[2] + " " + size[3] + " " + size[4]);
			int wrong = 0;
			for (int i = 0; i < n; i++) {
				if(out[i] != 4L * i + 1) {
					if(wrong < 5) {
						System.out.println("wrong at " + i + ": " + out[i]);
					}
					wrong++;
				}
			}
			System.out.println(wrong == 0 ? "prebuilt kernel OK with header " + header : "prebuilt kernel WRONG: " + wrong + " of " + n);
		}
		System.exit(0);
	}

	static Path write(String kernel) throws IOException {
		Path file = Files.createTempFile("probe", ".cl");
		Files.writeString(file, kernel, StandardCharsets.UTF_8);
		file.toFile().deleteOnExit();
		return file;
	}
}
