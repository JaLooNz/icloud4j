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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.StatusLine;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;

/**
 * A wrapper for the 'idmsa' service.
 */
public class IdmsaService
{

    public static final String idmsaEndPoint = "https://idmsa.apple.com";
    public static final String idmsaAuthEndPoint = "https://idmsa.apple.com/appleauth/auth";

    private final ICloudService iCloudService;
    private final CloseableHttpClient httpClient;
    private String appleIdSessionId;
    private String scnt;
    private String authToken;
    private String trustToken;

    public IdmsaService(ICloudService iCloudService)
    {
        this.iCloudService = iCloudService;
        this.httpClient = iCloudService.getHttpClient();
    }

    public StatusLine authenticateViaIdmsa(@Nonnull String username, @Nonnull char[] password)
    {
        try
        {
            Map<String, Object> params = ImmutableMap.of(
                "accountName", username,
                "password", new String(password),
                "rememberMe", false,
                "trustTokens", new String[0]
            );

            HttpPost post = new HttpPost(idmsaAuthEndPoint + "/signin");
            post.setEntity(new StringEntity(new Gson().toJson(params), ContentType.APPLICATION_JSON));
            populateIdmsaRequestHeadersParameters(post);
            post.setHeader("Accept", "application/json");

            HttpClientResponseHandler<StatusLine> handler = response -> {
                Header sessionTokenHeader = response.getFirstHeader("X-Apple-Session-Token");
                if (sessionTokenHeader != null) authToken = sessionTokenHeader.getValue();

                Header sessionHeader = response.getFirstHeader("X-Apple-ID-Session-Id");
                if (sessionHeader != null) appleIdSessionId = sessionHeader.getValue();

                Header scntHeader = response.getFirstHeader("scnt");
                if (scntHeader != null) scnt = scntHeader.getValue();

                return response.getCode() != 0 ? new StatusLine(response) : null;
            };

            return httpClient.execute(post, handler);

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private String sendIdmsaCode(String code) throws IOException
    {
        String json = String.format("{\"securityCode\":{\"code\":\"%s\"}}", code);
        HttpPost post = new HttpPost(idmsaAuthEndPoint + "/verify/trusteddevice/securitycode");
        post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        populateIdmsaRequestHeadersParameters(post);
        post.setHeader("Accept", "application/json");

        HttpClientResponseHandler<String> handler = response -> {
            if (response.getCode() >= 300)
                throw new IllegalStateException("Failed to verify code: " + response.getCode());
            return response.getFirstHeader("X-Apple-Session-Token").getValue();
        };

        return httpClient.execute(post, handler);
    }

    private String retrieveTrustToken() throws IOException
    {
        HttpGet get = new HttpGet(idmsaAuthEndPoint + "/2sv/trust");
        populateIdmsaRequestHeadersParameters(get);

        HttpClientResponseHandler<String> handler = response -> {
            if (response.getCode() >= 300)
                throw new IllegalStateException("Failed to get trust token: " + response.getCode());
            return response.getFirstHeader("X-Apple-TwoSV-Trust-Token").getValue();
        };

        return httpClient.execute(get, handler);
    }

    public void validateAutomaticVerificationCode(String code)
    {
        try
        {
            authToken = sendIdmsaCode(code);
            trustToken = retrieveTrustToken();

            Map<String, Object> params = ImmutableMap.of(
                "dsWebAuthToken", authToken,
                "trustToken", trustToken,
                "extended_login", false
            );
            iCloudService.authenticate(params);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void populateIdmsaRequestHeadersParameters(org.apache.hc.core5.http.ClassicHttpRequest request)
    {
        request.setHeader("Origin", idmsaEndPoint);
        request.setHeader("Referer", idmsaEndPoint + "/");
        request.setHeader("User-Agent", "Mozilla/5.0 (iPad; CPU OS 9_3_4 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13G35 Safari/601.1");
        request.setHeader("X-Apple-Widget-Key", "83545bf919730e51dbfba24e7e8a78d2");

        if (!Strings.isNullOrEmpty(appleIdSessionId))
            request.setHeader("X-Apple-ID-Session-Id", appleIdSessionId);
        if (!Strings.isNullOrEmpty(scnt))
            request.setHeader("scnt", scnt);
    }

    public String getAuthToken()
    {
        return authToken;
    }

    public String getTrustToken()
    {
        return trustToken;
    }
}
