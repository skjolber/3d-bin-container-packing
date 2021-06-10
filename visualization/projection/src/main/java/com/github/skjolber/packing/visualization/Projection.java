package com.github.skjolber.packing.visualization;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

public interface Projection<T> {

	PackagingVisualization project(List<T> input);

	void project(List<T> input, OutputStream out) throws Exception;

	void project(List<T> input, File output) throws Exception;
}
