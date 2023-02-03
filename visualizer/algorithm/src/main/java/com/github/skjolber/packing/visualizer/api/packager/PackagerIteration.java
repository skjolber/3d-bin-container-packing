package com.github.skjolber.packing.visualizer.api.packager;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;

public class PackagerIteration {

	private List<Box> inputBoxes = new ArrayList<>();

	private List<Container> outputContainers = new ArrayList<>();
	private List<Box> outputBoxes = new ArrayList<>();

	private List<PackagerOperation> operations = new ArrayList<>();

}
