package com.wirasat.util;

import java.io.IOException;
import java.util.Properties;

public class LoadProperties {
    public static Properties loadProperties(String path, ClassLoader classLoader) {
        Properties props = new Properties();
        try(var input = classLoader.getResourceAsStream(path)) {
            if (input == null) throw new RuntimeException("File not found: " + path);
            props.load(input);
        } catch(IOException e) {
            throw new RuntimeException("Failed to load properties: " + path, e);
        }
        return props;
    }
}
