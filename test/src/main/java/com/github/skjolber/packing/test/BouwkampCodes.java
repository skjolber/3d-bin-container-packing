package com.github.skjolber.packing.test;

import java.util.List;

public class BouwkampCodes {
	
	private List<BouwkampCode> codes;
	private String source;
	
	public BouwkampCodes(List<BouwkampCode> codes, String source) {
		super();
		this.codes = codes;
		this.source = source;
	}

	public void setCodes(List<BouwkampCode> codes) {
		this.codes = codes;
	}
	
	public List<BouwkampCode> getCodes() {
		return codes;
	}
}
