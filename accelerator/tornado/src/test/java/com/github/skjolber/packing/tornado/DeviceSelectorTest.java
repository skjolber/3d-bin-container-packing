package com.github.skjolber.packing.tornado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class DeviceSelectorTest {

	private static final long GIB = 1L << 30;

	private static DeviceInfo device(int backend, int index, String backendType, String name, String type, long memory, int computeUnits, int clockMHz) {
		return new DeviceInfo(backend, index, backendType, "platform", name, name + " description", type, memory, computeUnits, clockMHz);
	}

	// the integrated GPU shares the memory of the system, so it reports more of it than the discrete card
	private final DeviceInfo discrete = device(0, 0, "OPENCL", "gfx1102", "GPU", 8 * GIB, 16, 2600);
	private final DeviceInfo integrated = device(0, 1, "OPENCL", "gfx1036", "GPU", 46 * GIB, 2, 2200);
	private final DeviceInfo cpu = device(0, 2, "OPENCL", "cpu", "CPU", 64 * GIB, 32, 4500);
	private final DeviceInfo cuda = device(1, 0, "CUDA", "nvidia", "GPU", 24 * GIB, 80, 1700);

	@Test
	void choosesTheOpenCLGpuWithTheMostThroughputNotTheMostMemory() {
		assertThat(DeviceSelector.newBuilder().build().select(List.of(integrated, cpu, discrete, cuda))).contains(discrete);
		assertThat(DeviceSelector.newBuilder().build().select(List.of(discrete, integrated))).contains(discrete);
	}

	@Test
	void theMemoryDecidesBetweenEqualThroughput() {
		DeviceInfo small = device(0, 0, "OPENCL", "a", "GPU", 4 * GIB, 8, 2000);
		DeviceInfo large = device(0, 1, "OPENCL", "b", "GPU", 8 * GIB, 8, 2000);
		assertThat(DeviceSelector.newBuilder().build().select(List.of(small, large))).contains(large);
		assertThat(DeviceSelector.newBuilder().build().select(List.of(large, small))).contains(large);
	}

	@Test
	void backendAndTypeCanBeChanged() {
		List<DeviceInfo> devices = List.of(integrated, cpu, discrete, cuda);
		assertThat(DeviceSelector.newBuilder().withBackend("cuda").build().select(devices)).contains(cuda);
		assertThat(DeviceSelector.newBuilder().withType("CPU").build().select(devices)).contains(cpu);
		assertThat(DeviceSelector.newBuilder().withBackend(null).withType(null).build().select(devices)).contains(cpu);
	}

	@Test
	void choosesByNameIgnoringCase() {
		assertThat(DeviceSelector.newBuilder().withNameContains("GFX1036").build().select(List.of(discrete, integrated))).contains(integrated);
		// the description counts too
		assertThat(DeviceSelector.newBuilder().withNameContains("1102 DESCRIPTION").build().select(List.of(integrated, discrete))).contains(discrete);
	}

	@Test
	void excludesByName() {
		assertThat(DeviceSelector.newBuilder().withExcludedName("gfx1102").build().select(List.of(discrete, integrated))).contains(integrated);
		assertThat(DeviceSelector.newBuilder().withExcludedName("gfx").build().select(List.of(discrete, integrated))).isEmpty();
	}

	@Test
	void requiresAMinimumOfMemory() {
		assertThat(DeviceSelector.newBuilder().withMinGlobalMemory(16 * GIB).build().select(List.of(discrete))).isEmpty();
		assertThat(DeviceSelector.newBuilder().withMinGlobalMemory(16 * GIB).build().select(List.of(discrete, integrated))).contains(integrated);
		assertThat(DeviceSelector.newBuilder().withMinGlobalMemory(GIB).build().select(List.of(discrete, integrated))).contains(discrete);
	}

	@Test
	void theFirstDeviceWinsATie() {
		DeviceInfo first = device(0, 0, "OPENCL", "a", "GPU", 4 * GIB, 8, 2000);
		DeviceInfo second = device(0, 1, "OPENCL", "b", "GPU", 4 * GIB, 8, 2000);
		assertThat(DeviceSelector.newBuilder().build().select(List.of(first, second))).contains(first);
		assertThat(DeviceSelector.newBuilder().build().select(List.of(second, first))).contains(second);
	}

	@Test
	void noDevices() {
		assertThat(DeviceSelector.newBuilder().build().select(List.of())).isEmpty();
	}

	@Test
	void rejectsBadArguments() {
		assertThatThrownBy(() -> DeviceSelector.newBuilder().withExcludedName("")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> DeviceSelector.newBuilder().withExcludedName(null)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> DeviceSelector.newBuilder().withMinGlobalMemory(-1)).isInstanceOf(IllegalArgumentException.class);
	}
}
