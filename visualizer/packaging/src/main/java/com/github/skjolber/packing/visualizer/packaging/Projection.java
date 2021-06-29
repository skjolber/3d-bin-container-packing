package com.github.skjolber.packing.visualizer.packaging;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

import com.github.skjolber.packing.projection.PackagingVisualization;

public interface Projection<T> {

	PackagingVisualization project(List<T> input);

	void project(List<T> input, OutputStream out) throws Exception;

	void project(List<T> input, File output) throws Exception;
}
