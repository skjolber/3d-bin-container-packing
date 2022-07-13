package com.github.skjolber.packing.jmh;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.openjdk.jmh.runner.RunnerException;

import com.github.skjolber.packing.test.generator.Item;
import com.github.skjolber.packing.test.generator.ItemGenerator;
import com.github.skjolber.packing.test.generator.ItemIO;
import com.github.skjolber.packing.test.generator.egy.EgyItem;
import com.github.skjolber.packing.test.generator.egy.EgyItemGenerator;

public class EgyGeneratorTool {

    public static void main(String[] args) throws RunnerException, IOException {    	
		ItemGenerator<EgyItem> generator = new EgyItemGenerator(EgyItemGenerator.CLASS_1);
		
		List<EgyItem> items = generator.getItems(33);
		for(Item item : items) {
			System.out.println(item);
		}
		
		Path path = Paths.get("src","main","resources", "egy.json");
		
		ItemIO.write(path, items);
    }
	
}
