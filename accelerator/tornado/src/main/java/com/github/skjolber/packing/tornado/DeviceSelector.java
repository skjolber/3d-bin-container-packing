package com.github.skjolber.packing.tornado;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Chooses the device to run on: by default the OpenCL GPU with the most compute throughput (compute units times clock frequency), then the most global
 * memory. That is the discrete card on a machine which also has an integrated GPU; the memory of the latter is shared with the system, so it can be the larger. A
 * device can also be chosen by a part of its name, or excluded by one.
 */
public final class DeviceSelector {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		private String backend = "OPENCL";
		private String type = "GPU";
		private String nameContains;
		private final List<String> excluded = new ArrayList<>();
		private long minGlobalMemory;

		/**
		 * @param backend the type of the backend (OPENCL, CUDA, ...), or null for any; the default is OPENCL
		 * @return this builder
		 */
		public Builder withBackend(String backend) {
			this.backend = backend;
			return this;
		}

		/**
		 * @param type the type of the device (GPU, CPU, ACCELERATOR, ...), or null for any; the default is GPU
		 * @return this builder
		 */
		public Builder withType(String type) {
			this.type = type;
			return this;
		}

		/**
		 * @param nameContains a part of the name or description of the device (ignoring case), or null for any
		 * @return this builder
		 */
		public Builder withNameContains(String nameContains) {
			this.nameContains = nameContains;
			return this;
		}

		/**
		 * @param name a part of the name or description of a device which must not be chosen (ignoring case), for example an integrated GPU
		 * @return this builder
		 */
		public Builder withExcludedName(String name) {
			if(name == null || name.isEmpty()) {
				throw new IllegalArgumentException("Expected a name to exclude");
			}
			this.excluded.add(name);
			return this;
		}

		/**
		 * @param minGlobalMemory the least global memory of the device, in bytes
		 * @return this builder
		 */
		public Builder withMinGlobalMemory(long minGlobalMemory) {
			if(minGlobalMemory < 0) {
				throw new IllegalArgumentException("Expected zero or more bytes, got " + minGlobalMemory);
			}
			this.minGlobalMemory = minGlobalMemory;
			return this;
		}

		public DeviceSelector build() {
			return new DeviceSelector(backend, type, nameContains, List.copyOf(excluded), minGlobalMemory);
		}
	}

	private final String backend;
	private final String type;
	private final String nameContains;
	private final List<String> excluded;
	private final long minGlobalMemory;

	private DeviceSelector(String backend, String type, String nameContains, List<String> excluded, long minGlobalMemory) {
		this.backend = backend;
		this.type = type;
		this.nameContains = nameContains;
		this.excluded = excluded;
		this.minGlobalMemory = minGlobalMemory;
	}

	/**
	 * @param devices the devices of the runtime
	 * @return the device with the most throughput, then the most global memory (the first one on a tie), of those which match, or empty if none does
	 */
	public Optional<DeviceInfo> select(List<DeviceInfo> devices) {
		DeviceInfo best = null;
		for (DeviceInfo device : devices) {
			if(!matches(device)) {
				continue;
			}
			if(best == null || isBetter(device, best)) {
				best = device;
			}
		}
		return Optional.ofNullable(best);
	}

	private static boolean isBetter(DeviceInfo device, DeviceInfo best) {
		if(device.throughput() != best.throughput()) {
			return device.throughput() > best.throughput();
		}
		return device.globalMemory() > best.globalMemory();
	}

	private boolean matches(DeviceInfo device) {
		if(backend != null && !backend.equalsIgnoreCase(device.backend())) {
			return false;
		}
		if(type != null && !type.equalsIgnoreCase(device.type())) {
			return false;
		}
		if(device.globalMemory() < minGlobalMemory) {
			return false;
		}
		if(nameContains != null && !contains(device, nameContains)) {
			return false;
		}
		for (String name : excluded) {
			if(contains(device, name)) {
				return false;
			}
		}
		return true;
	}

	private static boolean contains(DeviceInfo device, String part) {
		String lower = part.toLowerCase(Locale.ROOT);
		return device.name().toLowerCase(Locale.ROOT).contains(lower) || device.description().toLowerCase(Locale.ROOT).contains(lower);
	}
}
