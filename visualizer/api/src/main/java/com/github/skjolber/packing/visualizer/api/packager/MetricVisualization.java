package com.github.skjolber.packing.visualizer.api.packager;

public abstract class MetricVisualization<T> extends PackagerOperation {

	public abstract String getType();
	
	public abstract <T> T getValue();
}
