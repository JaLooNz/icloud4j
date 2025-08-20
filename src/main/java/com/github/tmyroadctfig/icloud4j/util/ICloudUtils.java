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

package com.github.tmyroadctfig.icloud4j.util;

import com.github.tmyroadctfig.icloud4j.ICloudException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * iCloud utilities.
 */
public class ICloudUtils
{

    private static final Gson gson = new Gson();

    /**
     * Parses a JSON response from a POST request.
     */
    public static <T> T parseJsonResponse(CloseableHttpClient httpClient, HttpPost post, Class<T> responseClass)
    {
        try
        {
            return httpClient.execute(post, (ClassicHttpResponse response) -> {
                String rawResponseContent = new StringResponseHandler().handleResponse(response);

                try
                {
                    return gson.fromJson(rawResponseContent, responseClass);
                }
                catch (JsonSyntaxException e)
                {
                    Map<String, Object> errorMap = gson.fromJson(rawResponseContent, Map.class);
                    throw new ICloudException(response, errorMap);
                }
            });
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Parses a JSON response from a GET request.
     */
    public static <T> T parseJsonResponse(CloseableHttpClient httpClient, HttpGet get, Class<T> responseClass)
    {
        try
        {
            return httpClient.execute(get, (ClassicHttpResponse response) -> {
                String rawResponseContent = new StringResponseHandler().handleResponse(response);

                try
                {
                    return gson.fromJson(rawResponseContent, responseClass);
                }
                catch (JsonSyntaxException e)
                {
                    Map<String, Object> errorMap = gson.fromJson(rawResponseContent, Map.class);
                    throw new ICloudException(response, errorMap);
                }
            });
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }


    /**
     * Executes a GET request and returns the response body as an InputStream.
     * Caller is responsible for closing the InputStream.
     */
    public static InputStream executeStream(CloseableHttpClient client, HttpGet get)
    {
        try
        {
            return client.execute(get, response -> response.getEntity().getContent());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to execute HTTP GET for stream", e);
        }
    }
}

