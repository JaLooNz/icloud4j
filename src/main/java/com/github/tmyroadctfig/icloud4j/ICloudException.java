/*
 * Copyright 2016 Luke Quinane
 * Copyright 2025 JaLooNz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tmyroadctfig.icloud4j;

import com.google.common.collect.ImmutableMap;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.StatusLine;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An iCloud exception.
 *
 * @author Luke Quinanne
 */
public class ICloudException extends RuntimeException
{
    /**
     * The map of error values returned by iCloud.
     */
    private final Map<String, Object> errorMap;

    /**
     * The HTTP headers.
     */
    private final Map<String, String> headers;

    /**
     * The status line.
     */
    private final StatusLine statusLine;

    /**
     * Creates a new exception wrapping the original cause.
     *
     * @param response the HTTP response.
     * @param errorMap the map of error values returned by iCloud.
     */
    public ICloudException(HttpResponse response, Map<String, Object> errorMap)
    {
        this.errorMap = ImmutableMap.copyOf(errorMap);
        this.statusLine = new StatusLine(response);
        this.headers = Arrays
            .stream(response.getHeaders())
            .collect(Collectors.toMap(Header::getName, Header::getValue));
    }

    /**
     * Gets the map of error values returned by iCloud.
     *
     * @return the map.
     */
    public Map<String, Object> getErrorMap()
    {
        return errorMap;
    }

    /**
     * Gets the HTTP headers.
     *
     * @return the headers.
     */
    public Map<String, String> getHeaders()
    {
        return headers;
    }

    /**
     * Gets the status line.
     *
     * @return the status line.
     */
    public StatusLine getStatusLine()
    {
        return statusLine;
    }
}

