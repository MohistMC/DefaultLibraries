package com.mohistmc.libraries;

import com.mohistmc.libraries.plugin.LibraryLoader;
import java.net.URL;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LibraryLoaderTest {

    public static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        LibraryLoader queue = LibraryLoader.create("Test")
                .source("https://repo.maven.apache.org/maven2/")
                .libraries(List.of("com.squareup.okio:okio:3.5.0"))
                .build();

        for (URL url : queue.getJarFiles()) {
            LOGGER.info(url);
        }
    }
}
