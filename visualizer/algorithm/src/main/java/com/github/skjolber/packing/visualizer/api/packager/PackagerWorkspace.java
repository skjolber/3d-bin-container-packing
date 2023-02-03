package com.github.skjolber.packing.visualizer.api.packager;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;

public class PackagerWorkspace {

	private List<Container> newContainers = new ArrayList<>();

	private List<ContainerWorkspace> containers = new ArrayList<>();

	private List<Box> boxes = new ArrayList<>();

}
