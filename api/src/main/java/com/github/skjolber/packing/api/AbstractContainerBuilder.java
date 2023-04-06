package com.github.skjolber.packing.api;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@linkplain Stackable} builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

@SuppressWarnings("unchecked")
public class AbstractContainerBuilder<B extends AbstractContainerBuilder<B>> {

	protected String id;
	protected String description;

	protected BigDecimal dx = BigDecimal.valueOf(-1); // width
	protected BigDecimal dy = BigDecimal.valueOf(-1); // depth
	protected BigDecimal dz = BigDecimal.valueOf(-1); // height

	protected BigDecimal maxLoadWeight = BigDecimal.valueOf(-1);

	protected BigDecimal loadDx = BigDecimal.valueOf(-1); // x
	protected BigDecimal loadDy = BigDecimal.valueOf(-1); // y
	protected BigDecimal loadDz = BigDecimal.valueOf(-1); // z

	protected StackConstraint stackConstraint;

	protected List<Surface> surfaces;

	public B withSize(BigDecimal dx, BigDecimal dy, BigDecimal dz) {
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		return (B)this;
	}

	public B withMaxLoadWeight(BigDecimal weight) {
		this.maxLoadWeight = weight;
		return (B)this;
	}

	public B withLoadSize(BigDecimal dx, BigDecimal dy, BigDecimal dz) {
		this.loadDx = dx;
		this.loadDy = dy;
		this.loadDz = dz;
		return (B)this;
	}

	public B withStackConstraint(StackConstraint stackConstraint) {
		this.stackConstraint = stackConstraint;
		return (B)this;
	}

	public B withSurfaces(List<Surface> surfaces) {
		this.surfaces = surfaces;
		return (B)this;
	}

	public B withDescription(String description) {
		this.description = description;
		return (B)this;
	}

	public B withId(String id) {
		this.id = id;
		return (B)this;
	}

}
