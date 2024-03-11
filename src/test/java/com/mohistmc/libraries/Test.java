package com.mohistmc.libraries;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    public static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) throws IOException {
        LibrariesDownloadQueue queue = LibrariesDownloadQueue.create()
                .inputStream(Files.newInputStream(new File("libraries.txt").toPath()))
                .parentDirectory("libraries")
                .downloadSource("AUTO")
                .build();

        LOGGER.info(queue.toString());
        LOGGER.info("The library file is being detected...");
        LOGGER.info("Download the source: " + queue.downloadSource);

        queue.progressBar();

        LOGGER.info("The library file has been detected");
    }
}
