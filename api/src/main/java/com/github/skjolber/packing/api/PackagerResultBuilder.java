package com.github.skjolber.packing.api;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * {@linkplain PackagerResult} builder scaffold.
 * 
 */

@SuppressWarnings("unchecked")
public abstract class PackagerResultBuilder<B extends PackagerResultBuilder<B>> {

	protected List<StackableItem> items;
	protected long deadline = -1L;

	protected BooleanSupplier interrupt;

	protected int maxResults = -1;

	public B withItems(List<StackableItem> items) {
		this.items = items;
		return (B)this;
	}

	public B withDeadline(long deadline) {
		this.deadline = deadline;
		return (B)this;
	}

	public B withInterrupt(BooleanSupplier interrupt) {
		this.interrupt = interrupt;
		return (B)this;
	}

	public B withMaxResults(int maxResults) {
		this.maxResults = maxResults;
		return (B)this;
	}

	/**
	 * 
	 * Build result (perform packaging)
	 * 
	 * @return the result
	 */

	public abstract PackagerResult build();

}
