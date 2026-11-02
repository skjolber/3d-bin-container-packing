package com.github.skjolber.packing.tornado;

import java.util.ArrayList;
import java.util.List;

import uk.ac.manchester.tornado.api.TornadoBackend;
import uk.ac.manchester.tornado.api.TornadoRuntime;
import uk.ac.manchester.tornado.api.TornadoTargetDevice;
import uk.ac.manchester.tornado.api.common.TornadoDevice;
import uk.ac.manchester.tornado.api.runtime.TornadoRuntimeProvider;

/**
 * Lists the devices of the TornadoVM runtime. Without a runtime (the native drivers come with the TornadoVM installation, not with the API jar) the
 * list is empty, so that callers fall back to the CPU.
 */
public final class TornadoDevices {

	private static volatile Throwable lastFailure;

	private TornadoDevices() {
		// utility class
	}

	/**
	 * @return why the last {@link #discover()} found no runtime, or null if it found one
	 */
	public static Throwable getLastFailure() {
		return lastFailure;
	}

	/**
	 * @return the devices of all backends, or an empty list if there is no usable runtime
	 */
	public static List<DeviceInfo> discover() {
		List<DeviceInfo> devices = new ArrayList<>();
		try {
			TornadoRuntime runtime = TornadoRuntimeProvider.getTornadoRuntime();
			int backends = runtime.getNumBackends();
			for (int i = 0; i < backends; i++) {
				TornadoBackend backend = runtime.getBackend(i);
				String type = String.valueOf(backend.getBackendType());
				int deviceIndex = 0;
				for (TornadoDevice device : backend.getAllDevices()) {
					TornadoTargetDevice physical = device.getPhysicalDevice();
					devices.add(new DeviceInfo(i, deviceIndex, type, device.getPlatformName(), device.getDeviceName(), device.getDescription(), String.valueOf(device.getDeviceType()),
							device.getMaxGlobalMemory(), physical.getDeviceMaxComputeUnits(), physical.getDeviceMaxClockFrequency()));
					deviceIndex++;
				}
			}
		} catch (RuntimeException | LinkageError e) {
			// no runtime, or a runtime which does not match this version of the API
			lastFailure = e;
			return List.of();
		}
		lastFailure = null;
		return devices;
	}
}
