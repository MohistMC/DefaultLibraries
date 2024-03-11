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
                .downloadSource(null)
                .build();
        LOGGER.info(queue.toString());
        LOGGER.info("库文件检测中...");
        LOGGER.info("下载源: " + queue.downloadSource);
        queue.progressBar();
        LOGGER.info("库文件检测完毕");
    }
}
