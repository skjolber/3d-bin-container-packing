package com.github.skjolber.packing.api;

import java.io.Serializable;
import java.math.BigDecimal;

public class StackPlacement implements Placement3D, Serializable {

	private static final long serialVersionUID = 1L;

	protected Stackable stackable;
	protected StackValue value;

	protected BigDecimal x; // width coordinate
	protected BigDecimal y; // depth coordinate
	protected BigDecimal z; // height coordinate

	// TODO weight constraint
	protected BigDecimal maxSupportedPressure; // i.e.
	protected BigDecimal maxSupportedWeight;

	public StackPlacement(Stackable stackable, StackValue value, BigDecimal x, BigDecimal y, BigDecimal z, BigDecimal maxSupportedPressure,
						  BigDecimal maxSupportedWeight) {
		super();
		this.stackable = stackable;
		this.value = value;
		this.x = x;
		this.y = y;
		this.z = z;
		this.maxSupportedPressure = maxSupportedPressure;
		this.maxSupportedWeight = maxSupportedWeight;
	}

	public StackPlacement() {
	}

	public Stackable getStackable() {
		return stackable;
	}

	public void setStackable(Stackable stackable) {
		this.stackable = stackable;
	}

	public StackValue getStackValue() {
		return value;
	}

	public void setStackValue(StackValue stackValue) {
		this.value = stackValue;
	}

	public boolean intersects(StackPlacement placement) {
		return intersectsX(placement) && intersectsY(placement) && intersectsZ(placement);
	}

	public boolean intersectsY(StackPlacement placement) {
		BigDecimal startY = y;
		BigDecimal endY = startY.add(value.getDy()).subtract(BigDecimal.ONE);

		if(startY.compareTo(placement.getAbsoluteY()) <= 0 && placement.getAbsoluteY().compareTo(endY) <= 0) {
			return true;
		}

		return startY.compareTo(placement.getAbsoluteY().add(placement.getStackValue().getDy()).subtract(BigDecimal.ONE)) <= 0 &&
				placement.getAbsoluteY().add(placement.getStackValue().getDy()).subtract(BigDecimal.ONE).compareTo(endY) <= 0;
	}

	public boolean intersectsX(StackPlacement placement) {
		BigDecimal startX = x;
		BigDecimal endX = startX.add(value.getDx()).subtract(BigDecimal.ONE);

		if(startX.compareTo(placement.getAbsoluteX()) <= 0 && placement.getAbsoluteX().compareTo(endX) <= 0) {
			return true;
		}

		return startX.compareTo(placement.getAbsoluteX().add(placement.getStackValue().getDx()).subtract(BigDecimal.ONE)) <= 0 &&
				placement.getAbsoluteX().add(placement.getStackValue().getDx()).subtract(BigDecimal.ONE).compareTo(endX) <= 0;
	}

	public boolean intersectsZ(StackPlacement placement) {
		BigDecimal startZ = z;
		BigDecimal endZ = startZ.add(value.getDz()).subtract(BigDecimal.ONE);

		if(startZ.compareTo(placement.getAbsoluteZ()) <= 0 && placement.getAbsoluteZ().compareTo(endZ) <= 0) {
			return true;
		}

		return startZ.compareTo(placement.getAbsoluteZ().add(placement.getStackValue().getDz()).subtract(BigDecimal.ONE)) <= 0 &&
				placement.getAbsoluteZ().add(placement.getStackValue().getDz()).subtract(BigDecimal.ONE).compareTo(endZ) <= 0;
	}

	public BigDecimal getAbsoluteX() {
		return x;
	}

	public BigDecimal getAbsoluteY() {
		return y;
	}

	public BigDecimal getAbsoluteZ() {
		return z;
	}

	public BigDecimal getAbsoluteEndX() {
		return x.add(value.getDx()).subtract(BigDecimal.ONE);
	}

	public BigDecimal getAbsoluteEndY() {
		return y.add(value.getDy()).subtract(BigDecimal.ONE);
	}

	public BigDecimal getAbsoluteEndZ() {
		return z.add(value.getDz()).subtract(BigDecimal.ONE);
	}

	public BigDecimal getVolume() {
		return stackable.getVolume();
	}

	public boolean intersects2D(Placement2D point) {
		return !(point.getAbsoluteEndX().compareTo(x) < 0 || point.getAbsoluteX().compareTo(getAbsoluteEndX()) > 0 ||
				point.getAbsoluteEndY().compareTo(y) < 0 || point.getAbsoluteY().compareTo(getAbsoluteEndY()) > 0);
	}

	@Override
	public boolean intersects3D(Placement3D point) {
		return !(point.getAbsoluteEndX().compareTo(x) < 0 || point.getAbsoluteX().compareTo(getAbsoluteEndX()) > 0 ||
				point.getAbsoluteEndY().compareTo(y) < 0 || point.getAbsoluteY().compareTo(point.getAbsoluteEndY()) > 0
				|| point.getAbsoluteEndZ().compareTo(z) < 0 || point.getAbsoluteZ().compareTo(point.getAbsoluteEndZ()) > 0);
	}

	@Override
	public String toString() {
		return stackable.getDescription() + "[" + x + "x" + y + "x" + z + " " + getAbsoluteEndX() + "x" + getAbsoluteEndY() + "x" + getAbsoluteEndZ() + "]";
	}

	public void setX(BigDecimal x) {
		this.x = x;
	}

	public void setY(BigDecimal y) {
		this.y = y;
	}

	public void setZ(BigDecimal z) {
		this.z = z;
	}

	public void setValue(StackValue value) {
		this.value = value;
	}

	public void setMaxSupportedPressure(BigDecimal maxSupportedPressure) {
		this.maxSupportedPressure = maxSupportedPressure;
	}

	public void setMaxSupportedWeight(BigDecimal maxSupportedWeight) {
		this.maxSupportedWeight = maxSupportedWeight;
	}

	public BigDecimal getWeight() {
		return stackable.getWeight();
	}

}
