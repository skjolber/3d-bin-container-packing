package com.github.skjolber.packing.test.bouwkamp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// bowkampcodes
// http://www.squaring.net/sq/ss/siss/siss.html

public class BouwkampCodeParser {

	public BouwkampCodes parse(String resource) throws IOException {
		InputStream is = getClass().getResourceAsStream(resource);
		
		return new BouwkampCodes(parse(is, StandardCharsets.UTF_8), resource);
	}

	public List<BouwkampCode> parse(InputStream in, Charset charset) throws IOException {
		List<BouwkampCode> list = new ArrayList<>();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
		
		String str;
		while( (str = reader.readLine()) != null) {
			list.add(parseLine(str));
		}
		
		return list;
	}

	public BouwkampCode parseLine(String line) {
		BouwkampCode bkp = new BouwkampCode();
		
		Pattern pattern = Pattern.compile("(?<boxCount>[0-9]+)\\s(?<width>[0-9]+)\\s(?<depth>[0-9]+) (?<boxes>[\\(\\),0-9]+) \\* [0-9]+ : (?<name>[0-9A-Za-z]+)");
		// 16 48 48 (28,20)(8,12)(20,7,9)(5,7)(5,2)(3,11,2)(9)(8) * 16 : 48A
		Matcher matcher = pattern.matcher(line);
		if(!matcher.find()) {
			throw new BouwkampCodeException();
		}

		bkp.setWidth(Integer.parseInt(matcher.group("width")));
		bkp.setDepth(Integer.parseInt(matcher.group("depth")));
		bkp.setName(matcher.group("name"));
		
		String boxes = matcher.group("boxes");

		Pattern linePattern = Pattern.compile("\\([0-9,]+\\)");

		Matcher lineMatcher = linePattern.matcher(boxes);
		while(lineMatcher.find()) {
			
			String codeLine = boxes.substring(lineMatcher.start(), lineMatcher.end());
			
			List<Integer> squares = new ArrayList<>();
			
			Pattern boxPattern = Pattern.compile("[0-9]+");
			
			Matcher boxMatcher = boxPattern.matcher(codeLine);
			while(boxMatcher.find()) {
				squares.add(Integer.parseInt(codeLine.substring(boxMatcher.start(), boxMatcher.end())));
			}
			
			bkp.addLine(new BouwkampCodeLine(squares));
		}
		
		return bkp;
	}
	
	
}
