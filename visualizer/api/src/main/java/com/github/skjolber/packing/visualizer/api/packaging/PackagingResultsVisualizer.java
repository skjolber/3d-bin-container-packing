package com.github.skjolber.packing.visualizer.api.packaging;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The root of the viewer's JSON: one or more results, for example the results of several packagers for the same order,
 * which the viewer shows one at a time and compares.
 */
public class PackagingResultsVisualizer {

	private List<PackagingResultVisualizer> results = new ArrayList<>();

	public PackagingResultsVisualizer() {
	}

	public PackagingResultsVisualizer(PackagingResultVisualizer result) {
		results.add(result);
	}

	public List<PackagingResultVisualizer> getResults() {
		return results;
	}

	public void setResults(List<PackagingResultVisualizer> results) {
		this.results = results;
	}

	public boolean add(PackagingResultVisualizer result) {
		return results.add(result);
	}

	public String toJson() throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
	}
}
