package com.github.skjolber.packing.api.support;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement;

public class Support {

	protected Placement placement;
	protected List<TopBottomSupport> topSupports;
	protected List<TopBottomSupport> bottomSupports;
	
	protected long topArea;
	protected long bottomArea;

	protected long topWeight;
	
	public Support() {
		this.topSupports = new ArrayList<>();
		this.bottomSupports = new ArrayList<>();
	}

	public void setBottomPlacements(List<TopBottomSupport> bottomPlacements) {
		this.bottomSupports = bottomPlacements;
		
		long area = 0;
		for(TopBottomSupport support : bottomPlacements) {
			area += support.getArea();
		}
		this.bottomArea = area;
	}
	
	public List<TopBottomSupport> getBottomPlacements() {
		return bottomSupports;
	}
	
	public boolean isBottomPlacement(Placement placement) {
		if(bottomSupports == null) {
			return false;
		}
		// the top of the placement must be below the start of this placement
		//
		// |
		// |---------|
		// |         |
		// |  this   |
		// |         | |------------|
		// |---------| |            | |------------| 
		// |===========|     no     |=|    no      |=|------------|====> limit (inclusive)
		// |           |------------| |            | |    yes     |
		// |                          |------------| |            |
		// |                                         |------------| 
		// |
		//
		
		if(placement.getAbsoluteZ() <= placement.getAbsoluteEndZ()) {
			return false;
		}
		for(TopBottomSupport support : bottomSupports) {
			Placement bottom = support.getBottom();
			if(bottom == placement) {
				return true;
			}
			Support bottomSupport = bottom.getSupport();
			if(bottomSupport != null && bottomSupport.isBottomPlacement(placement)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isTopPlacement(Placement placement) {
		if(topSupports == null) {
			return false;
		}
		// the bottom of the placement must be above the end of this placement
		//
		// |          
		// |                                             |--------------|  
		// |                            |--------------| |              |
		// |           |--------------| |     no       | |     yes      |
		// |===========|      no      |=|              |=|--------------|=====> limit (inclusive)
		// |---------| |              | |--------------|
		// |         | |--------------|
		// |  this   |
		// |         |
		// |---------| 
		// |          
		//
		
		if(placement.getAbsoluteZ() > placement.getAbsoluteEndZ()) {
			return false;
		}
		for(TopBottomSupport support : topSupports) {
			Placement top = support.getTop();
			if(top == placement) {
				return true;
			}
			Support topSupport = top.getSupport();
			if(topSupport != null && topSupport.isTopPlacement(placement)) {
				return true;
			}
		}
		return false;
	}
	
	public void setTopPlacements(List<TopBottomSupport> topPlacements) {
		this.topSupports = topPlacements;
		
		long area = 0;
		long weight = 0;
		for(TopBottomSupport support : topPlacements) {
			area += support.getArea();
			weight += support.getWeight();
		}
		this.topArea = area;
		this.topWeight = weight;
	}
	
	public List<TopBottomSupport> getTopPlacements() {
		return topSupports;
	}
	
	public void addBottomPlacement(TopBottomSupport support) {
		this.bottomSupports.add(support);
		
		bottomArea += support.getArea();
	}
	
	public void addTopPlacement(TopBottomSupport support) {
		this.topSupports.add(support);
		
		topArea += support.getArea();
		topWeight += support.getWeight();
	}
	
	public long getTopArea() {
		return topArea;
		
	}
	public long getBottomArea() {
		return bottomArea;
	}
	
	public long getTopWeight() {
		return topWeight;
	}
	
	public long getBottomWeight() {
		return topWeight + placement.getWeight();
	}
	
	public void incrementTopWeight(long weight) {
		this.topWeight += weight;
	}
	
}
