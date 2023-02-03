package com.github.skjolber.packing.visualizer.packaging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.skjolber.packing.visualizer.api.packaging.PackagingResultVisualizer;

public abstract class AbstractPackagingResultVisualizerFactory<T> implements PackagingResultVisualizerFactory<T> {

	public void visualize(List<T> input, OutputStream out) throws Exception {
		PackagingResultVisualizer project = visualize(input);

		out.write(project.toJson().getBytes(StandardCharsets.UTF_8));
	}

	public void visualize(List<T> input, File output) throws Exception {
		if(!output.getParentFile().exists()) {
			if(!output.getParentFile().mkdirs()) {
				throw new IOException("Unable to create parent directory for " + output);
			}
		}
		FileOutputStream fout = new FileOutputStream(output);
		try {
			visualize(input, fout);
		} finally {
			fout.close();
		}
	}
}
