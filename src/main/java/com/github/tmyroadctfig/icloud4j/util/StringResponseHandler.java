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

import com.google.common.io.CharStreams;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * A response handler which returns the response content as a string.
 */
public class StringResponseHandler implements HttpClientResponseHandler<String> {

    @Override
    public String handleResponse(ClassicHttpResponse response) throws IOException {
        HttpEntity respEntity = response.getEntity();
        if (respEntity != null) {
            try (Reader reader = new InputStreamReader(respEntity.getContent(), StandardCharsets.UTF_8)) {
                return CharStreams.toString(reader);
            }
        }
        return null;
    }
}
