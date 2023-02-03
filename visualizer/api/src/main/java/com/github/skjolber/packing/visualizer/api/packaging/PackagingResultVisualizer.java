package com.github.skjolber.packing.visualizer.api.packaging;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PackagingResultVisualizer {

	private List<ContainerVisualizer> containers = new ArrayList<>();

	public List<ContainerVisualizer> getContainers() {
		return containers;
	}

	public void setContainers(List<ContainerVisualizer> containers) {
		this.containers = containers;
	}

	public boolean add(ContainerVisualizer e) {
		return containers.add(e);
	}

	public String toJson() throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();

		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
	}

}
