/*
 *    Copyright 2016 Luke Quinane
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.tmyroadctfig.icloud4j.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A response handler that converts a JSON response to a map of key/value pairs.
 *
 * @author Nick DS (me@nickdsantos.com)
 * @author Luke Quinane
 */
public class JsonToMapResponseHandler implements HttpClientResponseHandler<Map<String, Object>>
{
    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(JsonToMapResponseHandler.class);

    @Override
    public Map<String, Object> handleResponse(ClassicHttpResponse response) throws IOException
    {
        int statusCode = response.getCode();
        String reason = response.getReasonPhrase();
        if (logger.isDebugEnabled())
        {
            logger.debug("code: {} ; reason: {}", statusCode, reason);
        }

        HttpEntity respEntity = response.getEntity();
        if (respEntity != null)
        {
            Gson gson = new GsonBuilder().create();
            try (Reader reader = new InputStreamReader(respEntity.getContent(), StandardCharsets.UTF_8))
            {
                return gson.fromJson(reader, Map.class);
            }
        }

        return null;
    }
}