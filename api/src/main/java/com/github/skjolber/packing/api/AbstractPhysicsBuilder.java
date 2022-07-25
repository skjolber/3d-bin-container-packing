package com.github.skjolber.packing.api;


/**
 * Builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

@SuppressWarnings("unchecked")
public class AbstractPhysicsBuilder<B extends AbstractPhysicsBuilder<B>> {

	protected StackConstraint constraint;
	protected Dimension size;
	protected StackableSurface stackableSurface;

	public B withSize(int dx, int dy, int dz) {
		this.size = new Dimension(dx, dy, dz);
		
		return (B) this;
	}

	public B withRotate2D() {
		return withStackableSurface(StackableSurface.TWO_D);
	}

	public B withRotate3D() {
		return withStackableSurface(StackableSurface.THREE_D);
	}
	
	public B withStackableSurface(StackableSurface stackableSurface) {
		this.stackableSurface = stackableSurface;
		
		return (B) this;
	}

	public B withConstraint(StackConstraint stackConstraint) {
		this.constraint = stackConstraint;
		return (B)this;
	}

	
}
