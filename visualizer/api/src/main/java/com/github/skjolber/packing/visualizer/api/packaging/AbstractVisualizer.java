package com.github.skjolber.packing.visualizer.api.packaging;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.visualizer.api.VisualizerPlugin;

public class AbstractVisualizer {

	private int step;

	private List<VisualizerPlugin> plugins = new ArrayList<>();

	public void setStep(int step) {
		this.step = step;
	}

	public int getStep() {
		return step;
	}

	public void setPlugins(List<VisualizerPlugin> plugins) {
		this.plugins = plugins;
	}

	public List<VisualizerPlugin> getPlugins() {
		return plugins;
	}
}
