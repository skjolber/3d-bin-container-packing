package com.github.skjolber.packing.projection;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PackagingVisualization {

	private List<ContainerVisualization> containers = new ArrayList<>();

	public List<ContainerVisualization> getContainers() {
		return containers;
	}

	public void setContainers(List<ContainerVisualization> containers) {
		this.containers = containers;
	}

	public boolean add(ContainerVisualization e) {
		return containers.add(e);
	}
	
	public String toJson() throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
	}
	
}
