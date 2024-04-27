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

package com.mohistmc.libraries.plugin;

import com.mohistmc.mjson.Json;
import com.mohistmc.tools.ConnectionUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;

/**
 * Adopt a chain type
 */
public class LibraryLoader {

    private final Set<File> libraries = new HashSet<>();
    private final Set<Dependency> newDependencies = new HashSet<>();
    private final Set<DependencyIgnoreVersion> dependencyIgnoreVersion = new HashSet<>();
    private final String plugin;
    @Getter
    private final List<URL> jarFiles = new ArrayList<>();
    private String source;
    private List<String> desc = new ArrayList<>();
    private List<String> mohistLibs = new ArrayList<>();
    private File targetFile = new File("libraries", "plugins-lib");

    public LibraryLoader(String plugin) {
        this.plugin = plugin;
    }

    public static LibraryLoader create(String plugin) {
        return new LibraryLoader(plugin);
    }

    public LibraryLoader source(String source) {
        this.source = source;
        return this;
    }

    public LibraryLoader libraries(List<String> desc) {
        this.desc = desc;
        return this;
    }

    public LibraryLoader mohistLibs(List<String> mohistLibs) {
        this.mohistLibs = mohistLibs;
        return this;
    }

    public LibraryLoader targetFile(File targetFile) {
        this.targetFile = targetFile;
        return this;
    }

    public LibraryLoader build() {
        List<Dependency> dependencies = new ArrayList<>();
        for (String desc_libraries : desc) {
            String[] args = desc_libraries.split(":");
            if (args.length > 1) {
                Dependency dependency = new Dependency(args[0], args[1], args[2], false);
                if (has(dependency)) {
                    continue;
                }
                dependencies.add(dependency);
            }
        }

        for (Dependency dependency : dependencies) {
            String group = dependency.group().replace(".", "/");
            String fileName = "%s-%s.pom".formatted(dependency.name(), dependency.version());
            if (has(dependency)) {
                continue;
            }
            if (!mohistLibs.contains(fileName)) {
                if (dependency.version().equalsIgnoreCase("LATEST")) {
                    newDependencies.add(findDependency(group, dependency.name(), false));
                } else {
                    newDependencies.add(dependency);
                    String pomUrl = source + "%s/%s/%s/%s".formatted(group, dependency.name(), dependency.version(), fileName);
                    if (ConnectionUtil.isValid(pomUrl)) {
                        newDependencies.addAll(initDependencies0(pomUrl));
                    }
                }
            }
        }

        System.out.printf("[%s] Loading %s extra libraries... please wait%n", plugin, newDependencies.size() - desc.size());

        for (Dependency dependency : newDependencies) {
            String group = dependency.group().replace(".", "/");
            String fileName = "%s-%s.jar".formatted(dependency.name(), dependency.version());

            String mavenUrl = source + "%s/%s/%s/%s".formatted(group, dependency.name(), dependency.version(), fileName);
            File file = new File(targetFile, "%s/%s/%s/%s".formatted(group, dependency.name(), dependency.version(), fileName));

            if (has(dependency)) {
                continue;
            }
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();

                InputStream inputStream = new URL(mavenUrl).openStream();
                ReadableByteChannel rbc = Channels.newChannel(inputStream);
                FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

                fc.transferFrom(rbc, 0, Long.MAX_VALUE);
                fc.close();
                rbc.close();

                libraries.add(file);
            } catch (IOException ignored) {
            }
        }

        for (File file : libraries) {
            try {
                jarFiles.add(file.toURI().toURL());
                System.out.printf("[%s] Loaded libraries %s%n", plugin, file);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    public Set<Dependency> initDependencies0(String url) {
        Set<Dependency> list = new HashSet<>();
        for (Dependency dependency : initDependencies(url)) {
            if (newDependencies.contains(dependency)) {
                continue;
            }
            list.add(dependency);

            if (dependencyIgnoreVersion.contains(dependency.toIgnoreVersion())) {
                continue;
            }
            dependencyIgnoreVersion.add(dependency.toIgnoreVersion());
            if (dependency.extra()) {
                String group = dependency.group().replace(".", "/");
                String fileName = "%s-%s.pom".formatted(dependency.name(), dependency.version());
                String pomUrl = source + "%s/%s/%s/%s".formatted(group, dependency.name(), dependency.version(), fileName);
                if (ConnectionUtil.isValid(pomUrl)) list.addAll(initDependencies(pomUrl));
            }
        }
        return list;
    }

    public Set<Dependency> initDependencies(String url) {
        Set<Dependency> list = new HashSet<>();
        Json json = Json.readXml(url);
        if (json == null) return list;
        Json json2Json = json.at("project");
        String version = json2Json.has("parent") ? json2Json.at("parent").asString("version") : json2Json.asString("version");
        String groupId = json2Json.has("parent") ? json2Json.at("parent").asString("groupId") : json2Json.asString("groupId");

        if (!json2Json.has("dependencies")) return list;
        if (!json2Json.at("dependencies").toString().startsWith("{\"dependency\"")) return list;
        Json json3Json = json2Json.at("dependencies").at("dependency");
        if (json3Json.isArray()) {
            for (Json o : json2Json.at("dependencies").asJsonList("dependency")) {
                dependency(o, list, version, groupId);
            }
        } else {
            dependency(json3Json, list, version, groupId);
        }
        list.addAll(findDependency(list));
        return list;
    }

    public void dependency(Json json, Set<Dependency> list, String version, String parent_groupId) {
        try {
            if (json.has("groupId") && json.has("artifactId")) {
                String groupId = json.asString("groupId");
                if (groupId.startsWith("${")) {
                    groupId = parent_groupId;
                }
                String artifactId = json.asString("artifactId");
                DependencyIgnoreVersion d = new DependencyIgnoreVersion(groupId, artifactId);
                if (dependencyIgnoreVersion.contains(d)) {
                    return;
                }
                if (json.has("optional")) {
                    return;
                }
                if (json.has("scope") && (json.asString("scope").equals("test") || json.asString("scope").equals("provided"))) {
                    return;
                }
                if (json.has("version")) {
                    String versionAsString = json.asString("version");
                    if (versionAsString.contains("${project.version}") || versionAsString.contains("${project.parent.version}")) {
                        Dependency dependency = new Dependency(groupId, artifactId, version, true);
                        list.add(dependency);
                    } else if (!versionAsString.contains("${")) {
                        Dependency dependency = new Dependency(groupId, artifactId, versionAsString, true);
                        list.add(dependency);
                    }
                } else {
                    if (json.has("scope") && json.asString("scope").equals("compile")) {
                        list.add(findDependency(groupId, artifactId, true));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Dependency findDependency(String groupId, String artifactId, boolean extra) {
        String mavenUrl = source + "%s/%s/%s".formatted(groupId.replace(".", "/"), artifactId, "maven-metadata.xml");
        Json compile_json2Json = Json.readXml(mavenUrl).at("metadata");
        List<Object> v = compile_json2Json.at("versioning").at("versions").at("version").asList();
        return new Dependency(groupId, artifactId, (String) v.get(v.size() - 1), extra);
    }

    public Set<Dependency> findDependency(Set<Dependency> dependencySet) {
        Set<Dependency> list = new HashSet<>();
        for (Dependency dependency : dependencySet) {
            if (dependencyIgnoreVersion.contains(dependency.toIgnoreVersion())) {
                continue;
            }
            dependencyIgnoreVersion.add(dependency.toIgnoreVersion());
            String group = dependency.group.replace(".", "/");
            String fileName = "%s-%s.pom".formatted(dependency.name, dependency.version);
            String pomUrl = source + "%s/%s/%s/%s".formatted(group, dependency.name, dependency.version, fileName);
            if (ConnectionUtil.isValid(pomUrl)) {
                list.addAll(initDependencies(pomUrl));
            }
        }

        return list;
    }

    public boolean has(Dependency dependency) {
        String fileName = "%s-%s.jar".formatted(dependency.name(), dependency.version());
        File file = new File(targetFile, "%s/%s/%s/%s".formatted(dependency.group, dependency.name, dependency.version(), fileName));

        if (file.exists()) {
            if (!dependencyIgnoreVersion.contains(dependency.toIgnoreVersion())) {
                libraries.add(file);
                dependencyIgnoreVersion.add(dependency.toIgnoreVersion());
                System.out.printf("[%s] Found libraries %s%n", plugin, file);
                return true;
            }
        }
        return false;
    }

    public record Dependency(String group, String name, String version, boolean extra) {


        public DependencyIgnoreVersion toIgnoreVersion() {
            return new DependencyIgnoreVersion(group, name);
        }
    }

    public record DependencyIgnoreVersion(String group, String name) {
    }
}
