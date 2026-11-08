package com.github.djkingcraftero89.TH_TempFly.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static com.github.djkingcraftero89.TH_TempFly.config.ConfigUpdaterTest.yaml;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigUpdaterFileTest {
	private static final String BUNDLED = """
			config-version: 1
			fly:
			  interval: 20
			  mode: FLYING
			""";

	@TempDir
	Path dataFolder;

	private final List<LogRecord> logs = new ArrayList<>();
	private ConfigUpdater updater;

	@BeforeEach
	void setUp() {
		Logger logger = Logger.getAnonymousLogger();
		logger.setUseParentHandlers(false);
		logger.addHandler(new Handler() {
			@Override
			public void publish(LogRecord record) {
				logs.add(record);
			}

			@Override
			public void flush() {
			}

			@Override
			public void close() {
			}
		});
		updater = new ConfigUpdater(dataFolder.toFile(), "config.yml",
				() -> new ByteArrayInputStream(BUNDLED.getBytes(StandardCharsets.UTF_8)), logger);
	}

	@Test
	void outdatedFileIsBackedUpAndRewritten() throws IOException {
		byte[] original = write("fly:\n  interval: 40\n");

		assertTrue(updater.update());

		Path backup = singleBackup();
		assertTrue(backup.getFileName().toString().matches("config\\.yml\\.v0-\\d{8}-\\d{6}\\.bak"));
		assertArrayEquals(original, Files.readAllBytes(backup));
		var updated = yaml(Files.readString(file()));
		assertEquals(1, updated.getInt(ConfigUpdater.VERSION_KEY));
		assertEquals(40, updated.getInt("fly.interval"));
		assertEquals("FLYING", updated.getString("fly.mode"));
		assertFalse(Files.exists(dataFolder.resolve("config.yml.tmp")));
		assertTrue(logs.stream().anyMatch(log -> log.getLevel() == Level.INFO
				&& log.getMessage().contains("from version 0 to 1")
				&& log.getMessage().contains(backup.getFileName().toString())));
	}

	@Test
	void upToDateFileIsNotTouched() throws IOException {
		byte[] original = write(BUNDLED);

		assertFalse(updater.update());

		assertArrayEquals(original, Files.readAllBytes(file()));
		assertEquals(0, backupCount());
	}

	@Test
	void invalidYamlIsNotTouched() throws IOException {
		byte[] original = write("fly:\n  interval: [20\n");

		assertFalse(updater.update());

		assertArrayEquals(original, Files.readAllBytes(file()));
		assertEquals(0, backupCount());
		assertTrue(logs.stream().anyMatch(log -> log.getLevel() == Level.WARNING));
	}

	@Test
	void newerVersionIsNotTouched() throws IOException {
		byte[] original = write("config-version: 2\nfly:\n  interval: 40\n");

		assertFalse(updater.update());

		assertArrayEquals(original, Files.readAllBytes(file()));
		assertEquals(0, backupCount());
		assertTrue(logs.stream().anyMatch(log -> log.getLevel() == Level.WARNING));
	}

	@Test
	void missingFileIsLeftForTheCallerToCreate() {
		assertFalse(updater.update());
		assertFalse(Files.exists(file()));
	}

	private Path file() {
		return dataFolder.resolve("config.yml");
	}

	private byte[] write(String content) throws IOException {
		byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
		Files.write(file(), bytes);
		return bytes;
	}

	private Path singleBackup() throws IOException {
		try (Stream<Path> files = Files.list(dataFolder)) {
			List<Path> backups = files.filter(path -> path.toString().endsWith(".bak")).toList();
			assertEquals(1, backups.size());
			return backups.get(0);
		}
	}

	private long backupCount() throws IOException {
		try (Stream<Path> files = Files.list(dataFolder)) {
			return files.filter(path -> path.toString().endsWith(".bak")).count();
		}
	}
}
