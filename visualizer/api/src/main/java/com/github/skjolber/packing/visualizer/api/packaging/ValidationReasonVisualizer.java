package com.github.skjolber.packing.visualizer.api.packaging;

import java.util.ArrayList;
import java.util.List;

/**
 * A reason why the result is invalid, with the placements it concerns (if known).
 */
public class ValidationReasonVisualizer {

	/** Reason class name, for example ExcessiveLoadWeightReason */
	private String type;
	private int code;
	private String message;
	private List<PlacementReferenceVisualizer> placements = new ArrayList<>();

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<PlacementReferenceVisualizer> getPlacements() {
		return placements;
	}

	public void setPlacements(List<PlacementReferenceVisualizer> placements) {
		this.placements = placements;
	}
}
