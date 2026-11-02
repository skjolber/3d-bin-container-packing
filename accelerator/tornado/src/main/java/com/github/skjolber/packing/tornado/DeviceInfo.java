package com.github.skjolber.packing.tornado;

/**
 * A compute device which the TornadoVM runtime reports, as plain values so that a device can be chosen (and tested) without a runtime.
 *
 * @param backendIndex the index of the backend (the driver) in the runtime
 * @param deviceIndex the index of the device in its backend
 * @param backend the type of the backend, for example OPENCL
 * @param platform the name of the platform, for example the OpenCL platform
 * @param name the name of the device
 * @param description the description of the device
 * @param type the type of the device, for example GPU
 * @param globalMemory the global memory of the device, in bytes
 * @param computeUnits the number of compute units of the device
 * @param clockMHz the maximum clock frequency of the device, in MHz
 */
public record DeviceInfo(int backendIndex, int deviceIndex, String backend, String platform, String name, String description, String type, long globalMemory, int computeUnits, int clockMHz) {

	/**
	 * @return a rough measure of the compute throughput: the compute units times the clock frequency
	 */
	public long throughput() {
		return (long)computeUnits * clockMHz;
	}

	@Override
	public String toString() {
		return backend + " " + backendIndex + ":" + deviceIndex + " " + type + " '" + name + "' (" + description + ") on '" + platform + "', " + (globalMemory >> 20) + " MiB, " + computeUnits + " CU at "
				+ clockMHz + " MHz";
	}
}
