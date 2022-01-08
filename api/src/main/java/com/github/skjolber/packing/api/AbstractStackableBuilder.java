package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;


/**
 * {@linkplain Stackable} builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

public class AbstractStackableBuilder<B extends AbstractStackableBuilder<B>> extends AbstractPhysicsBuilder<B> {

	protected List<StackablePhysics> physics = new ArrayList<>();
	
	protected String name;

	public B withName(String name) {
		this.name = name;
		return (B)this;
	}

	
}
