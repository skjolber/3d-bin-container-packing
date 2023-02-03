package com.github.skjolber.packing.test.generator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.skjolber.packing.test.generator.egy.EgyItem;

public class ItemIO {

	public static void write(Path path, List<EgyItem> items) throws IOException {
		try (OutputStream os = Files.newOutputStream(path)) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(os, items);
		}
	}

	public static List<Item> read(Path path) throws IOException {
		try (InputStream in = Files.newInputStream(path)) {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(in, mapper.getTypeFactory().constructCollectionType(List.class, EgyItem.class));
		}
	}

}
