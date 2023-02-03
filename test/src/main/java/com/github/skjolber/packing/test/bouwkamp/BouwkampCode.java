package com.github.skjolber.packing.test.bouwkamp;

import java.util.ArrayList;
import java.util.List;

public class BouwkampCode {

	protected int width;
	protected int depth;
	protected String name;

	private List<BouwkampCodeLine> lines = new ArrayList<>();

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addLine(BouwkampCodeLine line) {
		this.lines.add(line);
	}

	public List<BouwkampCodeLine> getLines() {
		return lines;
	}

	public void setLines(List<BouwkampCodeLine> lines) {
		this.lines = lines;
	}

	public int getOrder() {
		int order = 0;
		for (BouwkampCodeLine bouwkampCodeLine : lines) {
			order += bouwkampCodeLine.getSquares().size();
		}
		return order;
	}

	@Override
	public String toString() {
		return "BouwkampCode [name=" + name + ", width=" + width + ", depth=" + depth + ", square=" + lines + "]";
	}

}
