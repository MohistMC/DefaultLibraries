package com.mohistmc.libraries;

import com.mohistmc.tools.ConnectionUtil;
import com.mohistmc.tools.MD5Util;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    public static final Set<Libraries> fail = new HashSet<>();
    private static final Set<Libraries> librariesSet = new HashSet<>();
    public static Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        if (System.getProperty("log4j.configurationFile") == null) {
            System.setProperty("log4j.configurationFile", "log4j2.xml");
        }

        run();
    }

    public static void run() {
        init();
        LOGGER.info("开始下载");
        Set<Libraries> need_download = new LinkedHashSet<>();
        for (Libraries libraries : librariesSet) {
            File lib = new File("libraries", libraries.path);
            if (lib.exists() && Objects.equals(MD5Util.get(lib), libraries.md5)) {
                continue;
            }
            need_download.add(libraries);
        }

        if (!need_download.isEmpty()) {
            LOGGER.info("下载进度:");
            Queue<Libraries> queue = new ConcurrentLinkedQueue<>(need_download);
            ProgressBarBuilder builder = new ProgressBarBuilder().setTaskName("")
                    .setStyle(ProgressBarStyle.ASCII)
                    .setUpdateIntervalMillis(100)
                    .setInitialMax(need_download.size());
            try (ProgressBar pb = builder.build()) {
                ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                List<Future<?>> futures = new ArrayList<>();

                AtomicInteger downloadedCount = new AtomicInteger(0);
                while (!queue.isEmpty()) {
                    Libraries lib = queue.poll();
                    if (lib == null) continue;
                    Runnable downloadTask = getRunnable(lib, pb, downloadedCount);
                    Future<?> future = executor.submit(downloadTask);
                    futures.add(future);
                }

                for (Future<?> future : futures) {
                    if (future != null) {
                        future.get();
                    }
                }

                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            } catch (ExecutionException | InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (!fail.isEmpty()) {
            run();
        } else {
            LOGGER.info("下载完毕");
        }
    }

    private static Runnable getRunnable(Libraries lib, ProgressBar pb, AtomicInteger downloadedCount) {
        File file = new File("libraries", lib.path);
        return () -> {
            try {
                file.getParentFile().mkdirs();
                String url = DownloadSource.fast(lib);
                ConnectionUtil.downloadFile(url, file);
                synchronized (pb) {
                    downloadedCount.addAndGet(1);
                    pb.step();
                }
                fail.remove(lib);
            } catch (Exception e) {
                if (e.getMessage() != null && !"md5".equals(e.getMessage())) {
                    file.delete();
                }
                fail.add(lib);
            }
        };
    }


    public static void init() {
        try {
            BufferedReader b = new BufferedReader(new InputStreamReader(Files.newInputStream(new File("libraries.txt").toPath())));
            for (String line = b.readLine(); line != null; line = b.readLine()) {
                Libraries libraries = Libraries.from(line);
                librariesSet.add(libraries);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
