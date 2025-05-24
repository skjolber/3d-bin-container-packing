package com.github.skjolber.packing.packer.bruteforce;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;

public interface BruteForcePackagerStrategy {

	boolean allPoints();
	
	boolean allRotations();
	
	boolean allPermtation();
	
	
	
}
