package com.github.skjolber.packing.packer.bruteforce.tree;

/**
 * A part of a search for a thread: the whole search, or a part stolen from another thread, which is the orders which start with a prefix of
 * types, placed at given candidates, and at the level after the prefix the types of a range of the type order.
 */
public final class BruteForceTreeWork {

	/** the whole search */
	public static final BruteForceTreeWork ROOT = new BruteForceTreeWork(new int[0], null, 0, Integer.MAX_VALUE);

	final int[] prefixTypes;
	/** per prefix level, the ordinal of the candidate placed (among the candidates of the type at the level), or null for every candidate */
	final int[] prefixOrdinals;
	/** the range of the type order at the level after the prefix */
	final int startRank;
	final int endRank;

	BruteForceTreeWork(int[] prefixTypes, int[] prefixOrdinals, int startRank, int endRank) {
		this.prefixTypes = prefixTypes;
		this.prefixOrdinals = prefixOrdinals;
		this.startRank = startRank;
		this.endRank = endRank;
	}

	/** @return whether this is a stolen part of another thread's search (the prefix placed at given candidates) */
	public boolean isStolen() {
		return prefixOrdinals != null;
	}
}
