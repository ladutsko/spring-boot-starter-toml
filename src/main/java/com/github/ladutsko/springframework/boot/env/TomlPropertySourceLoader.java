/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 George Ladutsko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.ladutsko.springframework.boot.env;

import com.electronwill.toml.Toml;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;

/**
 * Strategy to load TOML files into a {@link PropertySource}.
 *
 * @author <a href="mailto:ladutsko@gmail.com">George Ladutsko</a>
 */
public class TomlPropertySourceLoader implements PropertySourceLoader {

    /**
     * Returns the file extensions that the loader supports (excluding the '.').
     *
     * @return the file extensions
     */
    @Override
    public String[] getFileExtensions() {
        return new String[] { "toml" };
    }

    /**
     * Load the resource into a property source.
     *
     * @param name     the name of the property source
     * @param resource the resource to load
     * @param profile  the name of the profile to load or {@code null}. The profile can be
     *                 used to load multi-document files (such as YAML). Simple property formats should
     *                 {@code null} when asked to load a profile.
     * @return a property source or {@code null}
     * @throws IOException if the source cannot be loaded
     */
    @Override
    public PropertySource<?> load(String name, Resource resource, String profile) throws IOException {
        if (null == profile) {
            try (InputStream in = resource.getInputStream()) {
                Map<String, Object> source = Toml.read(in);
                if (!source.isEmpty()) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    buildFlattenedMap(result, source, null);
                    if (!result.isEmpty()) {
                        return new MapPropertySource(name, result);
                    }
                }
            }
        }

        return null;
    }

    private void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, String root) {
        boolean rootHasText = (null != root && !root.trim().isEmpty());

        source.forEach((key, value) -> {
            String path;

            if (rootHasText) {
                if (key.startsWith("[")) {
                    path = root + key;
                } else {
                    path = root + "." + key;
                }
            } else {
                path = key;
            }

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) value;
                buildFlattenedMap(result, map, path);
            } else if (value instanceof Collection) {
                @SuppressWarnings("unchecked")
                Collection<Object> collection = (Collection<Object>) value;
                int count = 0;
                for (Object object : collection) {
                    buildFlattenedMap(result, singletonMap("[" + (count++) + "]", object), path);
                }
            } else {
                result.put(path, null == value ? "" : value);
            }
        });
    }
}
