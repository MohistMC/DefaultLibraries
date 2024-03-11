package com.mohistmc.libraries;

import com.mohistmc.tools.ConnectionUtil;
import com.mohistmc.tools.MD5Util;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
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

public class LibrariesDownloadQueue {

    private final Set<Libraries> fail = new HashSet<>();
    private final Set<Libraries> librariesSet = new HashSet<>();
    public DownloadSource downloadSource;
    private String parentDirectory = "libraries";
    private InputStream in = null;

    public static LibrariesDownloadQueue create() {
        return new LibrariesDownloadQueue();
    }

    public LibrariesDownloadQueue inputStream(InputStream in) {
        this.in = in;
        return this;
    }

    public LibrariesDownloadQueue file(String parentDirectory) {
        this.parentDirectory = parentDirectory;
        return this;
    }

    public LibrariesDownloadQueue build() {
        downloadSource = DownloadSource.fast();
        init();
        return this;
    }

    public void progressBar() {
        Set<Libraries> need_download = new LinkedHashSet<>();
        for (Libraries libraries : librariesSet) {
            File lib = new File(parentDirectory, libraries.path);
            if (lib.exists() && Objects.equals(MD5Util.get(lib), libraries.md5)) {
                continue;
            }
            need_download.add(libraries);
        }

        if (!need_download.isEmpty()) {
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
            progressBar();
        }
    }

    private Runnable getRunnable(Libraries lib, ProgressBar pb, AtomicInteger downloadedCount) {
        File file = new File(parentDirectory, lib.path);
        return () -> {
            try {
                file.getParentFile().mkdirs();
                String url = downloadSource.url + lib.path;
                ConnectionUtil.downloadFile(url, file);
                synchronized (pb) {
                    downloadedCount.addAndGet(1);
                    pb.step();
                }
                fail.remove(lib);
            } catch (Exception e) {
                if (!MD5Util.get(file).equals(lib.md5)) {
                    file.delete();
                }
                fail.add(lib);
            }
        };
    }


    private void init() {
        try {
            BufferedReader b = new BufferedReader(new InputStreamReader(in));
            for (String line = b.readLine(); line != null; line = b.readLine()) {
                Libraries libraries = Libraries.from(line);
                librariesSet.add(libraries);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "LibrariesDownloadQueue(parentDirectory=" + this.parentDirectory + ", downloadSource=" + this.downloadSource + ")";
    }
}
