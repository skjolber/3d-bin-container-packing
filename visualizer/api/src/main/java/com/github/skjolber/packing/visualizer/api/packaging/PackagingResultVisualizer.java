package com.github.skjolber.packing.visualizer.api.packaging;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PackagingResultVisualizer {

	private List<ContainerVisualizer> containers = new ArrayList<>();

	// packager result, or null if visualizing containers only
	private Boolean success;
	private Boolean timeout;
	/** Packaging duration in milliseconds */
	private Long duration;
	/** Total container cost, -1 if not calculated */
	private Long cost;

	/** Whether the result passed validation */
	private boolean valid = true;
	private List<ValidationReasonVisualizer> validationReasons = new ArrayList<>();

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public List<ValidationReasonVisualizer> getValidationReasons() {
		return validationReasons;
	}

	public void setValidationReasons(List<ValidationReasonVisualizer> validationReasons) {
		this.validationReasons = validationReasons;
	}

	public Boolean getSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public Boolean getTimeout() {
		return timeout;
	}

	public void setTimeout(Boolean timeout) {
		this.timeout = timeout;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public Long getCost() {
		return cost;
	}

	public void setCost(Long cost) {
		this.cost = cost;
	}

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
