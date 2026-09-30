package com.automation.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads a .properties file from the classpath. Missing environment-specific files are
 * tolerated (returns an empty Properties) so that config.properties defaults still apply.
 */
public final class PropertiesLoader {

    private PropertiesLoader() {
    }

    public static Properties load(String classpathLocation) {
        Properties props = new Properties();
        try (InputStream is = PropertiesLoader.class.getClassLoader().getResourceAsStream(classpathLocation)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load properties file: " + classpathLocation, e);
        }
        return props;
    }
}
