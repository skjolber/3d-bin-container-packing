package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;


/**
 * {@linkplain Stackable} builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

public class AbstractContainerBuilder<B extends AbstractContainerBuilder<B>> {

	protected String id;
	protected String description;

	protected int dx = -1; // width
	protected int dy = -1; // depth
	protected int dz = -1; // height

	protected int maxLoadWeight = -1;

	protected int loadDx = -1; // x
	protected int loadDy = -1; // y
	protected int loadDz = -1; // z
	
	protected StackConstraint stackConstraint;

	protected List<Surface> surfaces;
	
	public B withSize(int dx, int dy, int dz) {
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		return (B) this;
	}
	
	public B withMaxLoadWeight(int weight) {
		this.maxLoadWeight = weight;
		return (B) this;
	}
	
	public B withLoadSize(int dx, int dy, int dz) {
		this.loadDx = dx;
		this.loadDy = dy;
		this.loadDz = dz;
		return (B) this;
	}

	public B withStackConstraint(StackConstraint stackConstraint) {
		this.stackConstraint = stackConstraint;
		return (B) this;
	}
	
	public B withSurfaces(List<Surface> surfaces) {
		this.surfaces = surfaces;
		return (B) this;
	}
	public B withDescription(String description) {
		this.description = description;
		return (B) this;
	}
	
	public B withId(String id) {
		this.id = id;
		return (B)this;
	}

}
