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
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DownloadSource {

    MOHISTMC("https://maven.mohistmc.com/libraries/"),
    CHINA("http://s1.devicloud.cn:25119/libraries/"),
    GITHUB("https://mohistmc.github.io/maven/");

    public final String url;

    public static DownloadSource fast() {
        List<String> all1 = Arrays.stream(values()).filter(downloadSource -> downloadSource != GITHUB).map(downloadSource -> downloadSource.url).collect(Collectors.toList());
        String fastURL = ConnectionUtil.fastURL(all1);

        if (Objects.equals(CHINA.url, fastURL)) {
            return CHINA;
        } else if (Objects.equals(MOHISTMC.url, fastURL)) {
            return MOHISTMC;
        } else {
            return GITHUB;
        }
    }
}
