package com.github.skjolber.packing.visualizer.api.packaging;

public class BoxVisualizer extends StackableVisualizer {

	private String type = "box";

	/** Per-result identity key; equal values represent the same box-item type. */
	protected Integer boxItemKey;
	protected long weight;
	protected Long maxLoadWeight;
	protected Double maxLoadPressure;
	protected Integer maxLoadBoxCount;
	protected Boolean maxLoadIdenticalOnly;
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getBoxItemKey() {
		return boxItemKey;
	}

	public void setBoxItemKey(Integer boxItemKey) {
		this.boxItemKey = boxItemKey;
	}

	public long getWeight() {
		return weight;
	}

	public void setWeight(long weight) {
		this.weight = weight;
	}

	public Long getMaxLoadWeight() {
		return maxLoadWeight;
	}

	public void setMaxLoadWeight(Long maxLoadWeight) {
		this.maxLoadWeight = maxLoadWeight;
	}

	public Double getMaxLoadPressure() {
		return maxLoadPressure;
	}

	public void setMaxLoadPressure(Double maxLoadPressure) {
		this.maxLoadPressure = maxLoadPressure;
	}

	public Integer getMaxLoadBoxCount() {
		return maxLoadBoxCount;
	}

	public void setMaxLoadBoxCount(Integer maxLoadBoxCount) {
		this.maxLoadBoxCount = maxLoadBoxCount;
	}

	public Boolean getMaxLoadIdenticalOnly() {
		return maxLoadIdenticalOnly;
	}

	public void setMaxLoadIdenticalOnly(Boolean maxLoadIdenticalOnly) {
		this.maxLoadIdenticalOnly = maxLoadIdenticalOnly;
	}
	
	
}
