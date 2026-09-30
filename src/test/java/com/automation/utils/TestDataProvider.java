package com.automation.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads test data (currently: candidate post IDs for performance test iteration) from
 * src/test/resources/testdata/posts.csv, so scenarios don't hardcode a single record.
 */
public final class TestDataProvider {

    private static final org.slf4j.Logger log = LoggerUtil.getLogger(TestDataProvider.class);

    private TestDataProvider() {
    }

    public static List<Integer> loadPostIds() {
        List<Integer> ids = new ArrayList<>();
        try (InputStream is = TestDataProvider.class.getClassLoader().getResourceAsStream("testdata/posts.csv")) {
            if (is == null) {
                log.warn("testdata/posts.csv not found on classpath, defaulting to post id 1");
                return List.of(1);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) {
                        header = false;
                        continue;
                    }
                    if (!line.isBlank()) {
                        ids.add(Integer.parseInt(line.trim()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load testdata/posts.csv: {}", e.getMessage());
            return List.of(1);
        }
        return ids.isEmpty() ? List.of(1) : ids;
    }
}
