package com.github.skjolber.packing.tornado;

import java.util.List;

/**
 * Prints the devices of the TornadoVM runtime, and the one which the default {@linkplain DeviceSelector} chooses: a smoke test of the runtime.
 */
public final class ListDevices {

	private ListDevices() {
		// main class
	}

	public static void main(String[] args) {
		List<DeviceInfo> devices = TornadoDevices.discover();
		if(devices.isEmpty()) {
			System.out.println("No TornadoVM devices (is the TornadoVM runtime on the module path?)");
			Throwable failure = TornadoDevices.getLastFailure();
			if(failure != null) {
				failure.printStackTrace(System.out);
			}
			return;
		}
		for (DeviceInfo device : devices) {
			System.out.println(device);
		}
		System.out.println("Chosen: " + DeviceSelector.newBuilder().build().select(devices).map(DeviceInfo::toString).orElse("none"));
	}
}
