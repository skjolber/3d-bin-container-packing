package com.github.skjolber.packing.visualizer.packaging;

import java.util.List;

import com.github.skjolber.packing.visualizer.api.packaging.PackagingResultVisualizer;

public interface PackagingResultVisualizerFactory<T> {

	PackagingResultVisualizer visualize(List<T> input);

}
