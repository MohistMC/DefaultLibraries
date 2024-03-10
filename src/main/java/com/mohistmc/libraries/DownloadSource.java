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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public enum DownloadSource {

    MOHISTMC_CHINA("http://s1.devicloud.cn:25119/libraries/"),
    BMCLAPI2("https://bmclapi2.bangbang93.com/maven/"),
    ALIYUN("https://maven.aliyun.com/repository/public/"),
    MAVEN2("https://repo.maven.apache.org/maven2/"),
    FORGE("https://files.minecraftforge.net/maven/"),
    MOJANG("https://libraries.minecraft.net/"),
    FABRICMC("https://maven.fabricmc.net/"),
    NEOFORGED("https://maven.neoforged.net/releases/"),
    MOHISTMC("https://maven.mohistmc.com/"),
    MOHISTMC_OLD("https://maven.mohistmc.com/libraries/"),
    GITHUB("https://mohistmc.github.io/maven/");

    private final String url;

    public static String fast(Libraries config) {
        String path = config.path;
        List<String> all = Arrays.stream(values()).map(downloadSource -> downloadSource.getUrl() + path).collect(Collectors.toList());
        return ConnectionUtil.fastURL(all);
    }
}
