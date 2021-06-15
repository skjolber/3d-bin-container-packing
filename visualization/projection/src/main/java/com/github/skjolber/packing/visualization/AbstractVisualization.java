package com.github.skjolber.packing.visualization;

import java.util.ArrayList;
import java.util.List;

public class AbstractVisualization {

	private int step;
	private String id;
	private String name;

	private List<VisualizationPlugin> plugins = new ArrayList<>();

	public void setStep(int step) {
		this.step = step;
	}
	
	public int getStep() {
		return step;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}


	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}

	public void setPlugins(List<VisualizationPlugin> plugins) {
		this.plugins = plugins;
	}
	
	public List<VisualizationPlugin> getPlugins() {
		return plugins;
	}
}
