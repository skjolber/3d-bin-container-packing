package com.github.skjolber.packing.visualizer.api.packager;

import java.util.List;

import com.github.skjolber.packing.api.Box;

public interface AlgorithmListener {
	
	ContainerFilter onStartContainerFilter(List<ContainerWorkspace> containers, String type);
	void onEndContainerFilter(ContainerFilter containerFilter, List<ContainerWorkspace> containers);

	// type: weight, size, other
	BoxFilter onStartBoxFilter(List<Box> boxes, String type);
	void onEndBoxFilter(BoxFilter boxFilter, List<Box> box);

	PlacementFilter onStartPlacement(List<ContainerWorkspace> containers, List<Box> boxes, String type);
	void onEndPlacement(PlacementFilter filter, ContainerWorkspace containerWorkspace, List<Room> rooms, Box box);

}
