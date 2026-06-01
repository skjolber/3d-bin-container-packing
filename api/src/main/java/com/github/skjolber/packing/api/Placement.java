package com.github.skjolber.packing.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.github.skjolber.packing.api.point.Point;

public class Placement implements Serializable {

	private static final long serialVersionUID = 1L;

	protected BoxStackValue stackValue;
	protected int x;
	protected int y;
	protected int z;
	
	protected int pointIndex;

	protected long supportedArea;

	// -----------------------------------------------------------------------
	// Box-load tracking
	// -----------------------------------------------------------------------

	protected List<PlacementLoad> supporters = new ArrayList<>(4);
	protected List<PlacementLoad> supportees = new ArrayList<>(4);
	/**
	 * Total weight of all boxes resting on top of this placement.
	 * Includes all boxes in the vertical stack above, adjusted for area-proportional distribution.
	 */
	protected long loadWeight;

	public Placement(BoxStackValue stackValue, int index, int x, int y, int z) {
		super();
		this.stackValue = stackValue;
		this.pointIndex = index;
		
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Placement(BoxStackValue stackValue, Point point) {
		this(stackValue, point.getIndex(), point.getMinX(), point.getMinY(), point.getMinZ());
	}

	public Placement() {
	}

	public BoxStackValue getStackValue() {
		return stackValue;
	}

	public void setStackValue(BoxStackValue stackValue) {
		this.stackValue = stackValue;
	}

	public boolean intersects(Placement placement) {
		return intersectsX(placement) && intersectsY(placement) && intersectsZ(placement);
	}

	public boolean intersectsY(Placement placement) {
		int endY = y + stackValue.getDy() - 1;

		if (y <= placement.getAbsoluteY() && placement.getAbsoluteY() <= endY) {
			return true;
		}

		return y <= placement.getAbsoluteY() + placement.getStackValue().getDy() - 1
				&& placement.getAbsoluteY() + placement.getStackValue().getDy() - 1 <= endY;
	}

	public boolean intersectsX(Placement placement) {
		int endX = x + stackValue.getDx() - 1;

		if (x <= placement.getAbsoluteX() && placement.getAbsoluteX() <= endX) {
			return true;
		}

		return x <= placement.getAbsoluteX() + placement.getStackValue().getDx() - 1
				&& placement.getAbsoluteX() + placement.getStackValue().getDx() - 1 <= endX;
	}

	public boolean intersectsZ(Placement placement) {
		int endZ = z + stackValue.getDz() - 1;

		if (z <= placement.getAbsoluteZ() && placement.getAbsoluteZ() <= endZ) {
			return true;
		}

		return z <= placement.getAbsoluteZ() + placement.getStackValue().getDz() - 1
				&& placement.getAbsoluteZ() + placement.getStackValue().getDz() - 1 <= endZ;
	}

	public int getAbsoluteX() {
		return x;
	}

	public int getAbsoluteY() {
		return y;
	}

	public int getAbsoluteZ() {
		return z;
	}

	public int getAbsoluteEndX() {
		return x + stackValue.getDx() - 1;
	}

	public int getAbsoluteEndY() {
		return y + stackValue.getDy() - 1;
	}

	public int getAbsoluteEndZ() {
		return z + stackValue.getDz() - 1;
	}

	public long getVolume() {
		return stackValue.getBox().getVolume();
	}
	
	public boolean intersects2D(int placementX, int placementEndX, int placementY, int placementEndY) {
		return !(
				placementEndX < x || placementX > getAbsoluteEndX() || 
				placementEndY < y || placementY > getAbsoluteEndY()
				);
	}

	public boolean intersects2D(Placement placement) {
		return !(
				placement.getAbsoluteEndX() < x || placement.getAbsoluteX() > getAbsoluteEndX() || 
				placement.getAbsoluteEndY() < y || placement.getAbsoluteY() > getAbsoluteEndY()
				);
	}

	public boolean intersects3D(Placement placement) {
		return !(
				placement.getAbsoluteEndX() < x ||
				placement.getAbsoluteX() > getAbsoluteEndX() ||
				placement.getAbsoluteEndY() < y ||
				placement.getAbsoluteY() > getAbsoluteEndY() ||
				placement.getAbsoluteEndZ() < z ||
				placement.getAbsoluteZ() > getAbsoluteEndZ()
				);
	}

	@Override
	public String toString() {		
		Box box = stackValue.getBox();
		return (box != null ? box.getId() : "") + "[" +x + "x" + y + "x" + z + " " + getAbsoluteEndX() + "x"
				+ getAbsoluteEndY() + "x" + getAbsoluteEndZ() + "]";
	}
	
	public void setPoint(int index, int x, int y, int z) {
		this.pointIndex = index;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void setPoint(Point point) {
		setPoint(point.getIndex(), point.getMinX(), point.getMinY(), point.getMinZ());
	}
	
	public int getWeight() {
		return stackValue.getBox().getWeight();
	}

	public BoxItem getBoxItem() {
		return stackValue.getBox().getBoxItem();
	}
	
	public Box getBox() {
		return stackValue.getBox();
	}

	public int getPointIndex() {
		return pointIndex;
	}
	
	/**
	 * Total weight of all boxes resting on top of this placement.
	 * Includes all boxes in the vertical stack above, adjusted for area-proportional distribution.
	 *
	 * @return accumulated load weight, in the same units as {@link Box#getWeight()}
	 */
	public long getLoadWeight() {
		return loadWeight;
	}

	/**
	 * Returns the load pressure on the top surface of this placement,
	 * expressed as {@code loadWeight × 1000 / topArea}, matching the
	 * convention used by {@link Box#getMinimumPressure()}.
	 *
	 * @return load pressure, or 0 if the area is zero
	 */
	public long getLoadPressure() {
		long area = stackValue.getArea();
		if(area == 0) {
			return 0;
		}
		return (loadWeight * 1000L) / area;
	}

	/**
	 * Returns the list of placements that are directly supported by this placement.
	 *
	 * @return list of supportees
	 */
	public List<PlacementLoad> getSupportees() {
		return supportees;
	}

	/**
	 * Returns the list of placements that are directly supporting this placement.
	 *
	 * @return list of supporters
	 */
	public List<PlacementLoad> getSupporters() {
		return supporters;
	}

	public long getSupportdArea() {
		return supportedArea;
	}
	
	/**
	 * Records that {@code supportee} is resting on top of this placement.
	 * Sets up a two-way relationship and propagates weight and stack levels 
	 * down through the support graph.
	 *
	 * @param supportee the placement resting on top
	 * @param area the area shared between the two
	 * @param weight the initial weight share of the supportee box itself
	 */
	public void addLoad(Placement supportee, long area, long weight) {
		addSupportee(new PlacementLoad(supportee, area, weight));
		supportee.addSupporter(new PlacementLoad(this, area, weight));

		propagateLoad(weight);
	}
	
	protected void addSupportee(PlacementLoad supporter) {
		this.supportees.add(supporter);
	}

	protected void addSupporter(PlacementLoad supporter) {
		this.supporters.add(supporter);
		
		supportedArea += supporter.getArea();
	}

	protected void propagateLoad(long weightIncrement) {
		this.loadWeight += weightIncrement;

		if(!supporters.isEmpty()) {
			for (int i = 0; i < supporters.size(); i++) {
				PlacementLoad supporterLink = supporters.get(i);
				long share = (weightIncrement * supporterLink.getArea()) / supportedArea;
				supporterLink.getPlacement().propagateLoad(share);
			}
		}
	}
	
	public void removeLoad(Placement supportee) {
		PlacementLoad toRemove = null;
		for(PlacementLoad supporteeLink : supportees) {
			if(supporteeLink.getPlacement() == supportee) {
				toRemove = supporteeLink;
				break;
			}
		}
		
		if(toRemove != null) {
			supportees.remove(toRemove);
			supportee.removeSupporter(this);
			
			propagateLoad(-toRemove.getWeight());
		}
	}
	
	public void clearLoad() {
		supportees.clear();
		supporters.clear();
		
		loadWeight = 0;
		supportedArea = 0;
	}

	public void removeSupporter(Placement placement) {
		for(int i = 0; i < supporters.size(); i++) {
			PlacementLoad supporterLink = supporters.get(i);
			if(supporterLink.getPlacement() == placement) {
				supporters.remove(i);
				supportedArea -= supporterLink.getArea();
				
				propagateLoad(-supporterLink.getWeight());
				break;
			}
		}
	}

	public long getSupportedArea() {
		return supportedArea;
	}

	public void setSupportedArea(long supportedArea) {
		this.supportedArea = supportedArea;
	}
	
}