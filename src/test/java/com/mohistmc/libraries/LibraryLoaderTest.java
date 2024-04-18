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
                //.source("https://repo.maven.apache.org/maven2/")
                .source("https://maven.aliyun.com/repository/public/")
                .libraries(List.of("com.vk.api:sdk:1.0.14"))
                .build();

        for (URL url : queue.getJarFiles()) {
            LOGGER.info(url);
        }
    }
}
