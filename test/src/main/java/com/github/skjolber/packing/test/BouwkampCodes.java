package com.github.skjolber.packing.test;

import java.util.List;

public class BouwkampCodes {
	
	protected List<BouwkampCode> codes;
	protected String source;
	
	public BouwkampCodes(List<BouwkampCode> codes, String source) {
		super();
		this.codes = codes;
		this.source = source;
	}

	public void setCodes(List<BouwkampCode> codes) {
		this.codes = codes;
	}
	
	public BouwkampCode findCode(String name) {
		for (BouwkampCode bouwkampCode : codes) {
			if(bouwkampCode.getName().equals(name)) {
				return bouwkampCode;
			}
		}
		return null;
	}
	
	public List<BouwkampCode> getCodes() {
		return codes;
	}
	
	public String getSource() {
		return source;
	}
}
