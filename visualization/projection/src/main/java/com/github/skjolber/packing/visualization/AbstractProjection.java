package com.github.skjolber.packing.visualization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class AbstractProjection<T> implements Projection<T> {

	@Override
	public void project(List<T> input, OutputStream out) throws Exception {
		PackagingVisualization project = project(input);
		
		out.write(project.toJson().getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public void project(List<T> input, File output) throws Exception {
		FileOutputStream fout = new FileOutputStream(output);
		try {
			project(input,fout);
		} finally {
			fout.close();
		}
	}

}
