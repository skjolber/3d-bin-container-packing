package com.github.skjolber.packing.api;

/**
 * Builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

@SuppressWarnings("unchecked")
public class AbstractPhysicsBuilder<B extends AbstractPhysicsBuilder<B>> {

	protected int dx = -1;
	protected int dy = -1;
	protected int dz = -1;
	protected StackableSurface stackableSurface;

	public B withSize(int dx, int dy, int dz) {
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;

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

}
