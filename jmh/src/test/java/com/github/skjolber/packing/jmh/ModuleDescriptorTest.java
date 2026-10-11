package com.github.skjolber.packing.jmh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

/**
 * The library jars must be usable on the module path: the module descriptors (added when
 * packaging) must be valid, and every package with public classes must be exported.
 * <p>
 * Skipped when a library is not a packaged jar (i.e. its classes directory is used).
 */
public class ModuleDescriptorTest {

	@Test
	public void api() throws Exception {
		check("com.github.skjolber.packing.api.Box");
	}

	@Test
	public void points() throws Exception {
		check("com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D");
	}

	@Test
	public void core() throws Exception {
		check("com.github.skjolber.packing.packer.plain.PlainPackager");
	}

	@Test
	public void validators() throws Exception {
		check("com.github.skjolber.packing.validator.DefaultValidator");
	}

	private static void check(String className) throws Exception {
		Path jar = location(Class.forName(className));
		assumeTrue(Files.isRegularFile(jar), "Not a packaged jar: " + jar);

		// throws for an invalid descriptor, e.g. an exported package which does not exist
		Set<ModuleReference> modules = ModuleFinder.of(jar).findAll();
		assertThat(modules).hasSize(1);
		ModuleDescriptor descriptor = modules.iterator().next().descriptor();
		assertThat(descriptor.isAutomatic()).as("automatic module " + descriptor.name()).isFalse();

		Set<String> exported = new TreeSet<>();
		for(ModuleDescriptor.Exports exports : descriptor.exports()) {
			exported.add(exports.source());
		}
		Set<String> publicPackages = publicPackages(jar);
		publicPackages.removeAll(exported);
		assertThat(publicPackages).as("public packages not exported by " + descriptor.name()).isEmpty();
	}

	private static Path location(Class<?> type) throws URISyntaxException {
		return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
	}

	private static Set<String> publicPackages(Path jar) throws IOException, ClassNotFoundException {
		Set<String> packages = new TreeSet<>();
		try (JarFile file = new JarFile(jar.toFile())) {
			Enumeration<JarEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				String name = entries.nextElement().getName();
				if(!name.endsWith(".class") || name.contains("$") || name.endsWith("module-info.class") || name.startsWith("META-INF/")) {
					continue;
				}
				String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');
				Class<?> type = Class.forName(className, false, ModuleDescriptorTest.class.getClassLoader());
				if(Modifier.isPublic(type.getModifiers())) {
					packages.add(type.getPackageName());
				}
			}
		}
		return packages;
	}
}
