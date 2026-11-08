package com.github.djkingcraftero89.TH_TempFly.config;

import com.github.djkingcraftero89.TH_TempFly.config.ConfigUpdater.Decision;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.github.djkingcraftero89.TH_TempFly.config.ConfigUpdaterTest.yaml;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the YAML files shipped in the jar against what the updater relies on. */
class BundledResourcesTest {

	@ParameterizedTest
	@ValueSource(strings = {"config.yml", "messages.yml"})
	void versionKeyIsTheFirstKey(String name) throws IOException {
		YamlConfiguration bundled = yaml(read(name));

		assertEquals(ConfigUpdater.VERSION_KEY, bundled.getKeys(false).iterator().next());
		assertTrue(ConfigUpdater.readVersion(bundled) >= 1);
	}

	// Bukkit drops keys without a value on load, so the rebuild would silently lose them and their comments.
	@ParameterizedTest
	@ValueSource(strings = {"config.yml", "messages.yml"})
	void hasNoKeysWithoutAValue(String name) throws IOException {
		Map<?, ?> raw = new Yaml().load(read(name));
		List<String> nullKeys = new ArrayList<>();
		collectNullKeys(raw, "", nullKeys);

		assertEquals(List.of(), nullKeys);
	}

	@ParameterizedTest
	@ValueSource(strings = {"config.yml", "messages.yml"})
	void unversionedCopyIsRebuiltWithEveryValueAndComment(String name) throws IOException {
		String text = read(name);
		YamlConfiguration defaults = yaml(text);
		YamlConfiguration user = yaml(text.replaceFirst("(?m)^config-version: \\d+\\R", ""));

		YamlConfiguration result = yaml(ConfigUpdater.upgrade(yaml(text), user, new TreeMap<>()).saveToString());

		assertEquals(List.copyOf(defaults.getKeys(true)), List.copyOf(result.getKeys(true)));
		for (String path : defaults.getKeys(true)) {
			if (!defaults.isConfigurationSection(path)) {
				assertEquals(defaults.get(path), result.get(path), path);
			}
			assertEquals(defaults.getComments(path), result.getComments(path), path);
		}
		assertEquals(defaults.options().getHeader(), result.options().getHeader());
		assertEquals(Decision.UP_TO_DATE, ConfigUpdater.decide(defaults, result));
	}

	private static void collectNullKeys(Map<?, ?> map, String prefix, List<String> out) {
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			String path = prefix + entry.getKey();
			if (entry.getValue() == null) {
				out.add(path);
			} else if (entry.getValue() instanceof Map<?, ?> child) {
				collectNullKeys(child, path + ".", out);
			}
		}
	}

	private static String read(String name) throws IOException {
		try (InputStream in = BundledResourcesTest.class.getClassLoader().getResourceAsStream(name)) {
			assertNotNull(in, name + " is not on the test classpath");
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
