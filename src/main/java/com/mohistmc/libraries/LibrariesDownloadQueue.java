/*
 * Mohist - MohistMC
 * Copyright (C) 2018-2024.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
import lombok.ToString;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

@ToString
public class LibrariesDownloadQueue {

    @ToString.Exclude
    private final Set<Libraries> fail = new HashSet<>();
    @ToString.Exclude
    private final Set<Libraries> librariesSet = new HashSet<>();
    @ToString.Exclude
    private InputStream inputStream = null;

    public DownloadSource downloadSource = null;
    public int threadPoolSize = Runtime.getRuntime().availableProcessors();
    private String parentDirectory = "libraries";
    public String systemProperty = null;


    public static LibrariesDownloadQueue create() {
        return new LibrariesDownloadQueue();
    }

    /**
     * Set the input stream for the list that needs to be downloaded
     *
     * @param inputStream The input stream of the target file
     * @return Returns the current real column
     */
    public LibrariesDownloadQueue inputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    /**
     * Set the file download directory
     *
     * @param parentDirectory The path to which the file is downloaded
     * @return Returns the current real column
     */
    public LibrariesDownloadQueue parentDirectory(String parentDirectory) {
        this.parentDirectory = parentDirectory;
        return this;
    }

    /**
     * Set up a custom download source
     *
     * @param downloadSource You can get the enumeration name in the configuration file or customize the system attributes
     * @return Returns the current real column
     */
    public LibrariesDownloadQueue downloadSource(String downloadSource) {
        try {
            this.downloadSource = DownloadSource.valueOf(downloadSource);
        } catch (Exception e) {
            if (ConnectionUtil.isValid(downloadSource) && ConnectionUtil.canAccess(downloadSource)) {
                this.systemProperty = downloadSource;
            }
        }
        return this;
    }

    /**
     * Set the thread pool size
     *
     * @param threadPoolSize Allows you to customize the size of the download thread pool
     * @return Returns the current real column
     */
    public LibrariesDownloadQueue threadPoolSize(int threadPoolSize) {
        this.threadPoolSize = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), threadPoolSize));;
        return this;
    }

    /**
     * Construct the final column
     * @return Construct the final column
     */
    public LibrariesDownloadQueue build() {
        if (downloadSource == null) {
            downloadSource = DownloadSource.fast();
        }
        init();
        return this;
    }

    /**
     * Download in the form of a progress bar
     */
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
                ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
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
                String url;
                if (this.systemProperty != null) {
                    url = this.systemProperty + lib.path;
                } else {
                    url = this.downloadSource.url + lib.path;
                }
                ConnectionUtil.downloadFile(url, file);
                synchronized (pb) {
                    downloadedCount.addAndGet(1);
                    pb.step();
                }
                fail.remove(lib);
            } catch (Exception e) {
                if (!Objects.equals(MD5Util.get(file), lib.md5)) {
                    file.delete();
                }
                fail.add(lib);
            }
        };
    }


    private void init() {
        try {
            BufferedReader b = new BufferedReader(new InputStreamReader(inputStream));
            for (String line = b.readLine(); line != null; line = b.readLine()) {
                Libraries libraries = Libraries.from(line);
                librariesSet.add(libraries);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
