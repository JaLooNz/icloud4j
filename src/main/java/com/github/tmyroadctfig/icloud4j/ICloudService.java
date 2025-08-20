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

import com.github.tmyroadctfig.icloud4j.json.TrustedDevice;
import com.github.tmyroadctfig.icloud4j.json.TrustedDeviceResponse;
import com.github.tmyroadctfig.icloud4j.json.TrustedDevices;
import com.github.tmyroadctfig.icloud4j.util.JsonToMapResponseHandler;
import com.github.tmyroadctfig.icloud4j.util.StringResponseHandler;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The iCloud service.
 *
 * @author Luke Quinane
 */
public class ICloudService implements java.io.Closeable
{
    /**
     * The end point.
     */
    public static final String endPoint = "https://www.icloud.com";
    /**
     * The setup end point.
     */
    public static final String setupEndPoint = "https://setup.icloud.com/setup/ws/1";
    /**
     * A flag indicating whether to disable SSL checks.
     */
    private static final boolean DISABLE_SSL_CHECKS = Boolean.parseBoolean(System.getProperty("tmyroadctfig.icloud4j.disableSslChecks", "false"));
    /**
     * The proxy host to use.
     */
    private static final String PROXY_HOST = System.getProperty("http.proxyHost");
    /**
     * The proxy port to use.
     */
    private static final Integer PROXY_PORT = Integer.getInteger("http.proxyPort");
    /**
     * The client ID.
     */
    private final String clientId;

    /**
     * The HTTP client.
     */
    private final CloseableHttpClient httpClient;

    /**
     * The cookie store.
     */
    private final CookieStore cookieStore;

    /**
     * The idmsa service.
     */
    private final IdmsaService idmsaService;

    /**
     * The login info.
     */
    private Map<String, Object> loginInfo;

    /**
     * The iCloud session ID.
     */
    private String dsid;

    /**
     * Creates a new iCloud service instance.
     *
     * @param clientId the client ID.
     */
    public ICloudService(@Nonnull String clientId)
    {
        this(clientId, null);
    }

    /**
     * Creates a new iCloud service instance.
     *
     * @param clientId   the client ID.
     * @param httpClient the closeable http client.
     */
    public ICloudService(@Nonnull String clientId, @Nullable CloseableHttpClient httpClient)
    {
        this.clientId = clientId;

        cookieStore = new BasicCookieStore();

        if (httpClient != null)
        {
            this.httpClient = httpClient;
        } else
        {
            try
            {
                HttpClientBuilder clientBuilder = HttpClients.custom()
                    .setDefaultCookieStore(cookieStore);

                // Handle proxy if defined
                if (!Strings.isNullOrEmpty(PROXY_HOST) && PROXY_PORT != null)
                {
                    clientBuilder.setProxy(new HttpHost(PROXY_HOST, PROXY_PORT));
                }

                // Handle optional SSL checks
                if (DISABLE_SSL_CHECKS)
                {
                    SSLContext sslContext = SSLContextBuilder.create()
                        .loadTrustMaterial(null, (chain, authType) -> true) // trust all
                        .build();

                    clientBuilder.setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setTlsSocketStrategy(ClientTlsStrategyBuilder.create()
                            .setSslContext(sslContext)
                            //.setHostnameVerifier(NoopHostnameVerifier.INSTANCE) // for debugging only
                            .buildClassic())
                        .build());
                }

                this.httpClient = clientBuilder.build();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        idmsaService = new IdmsaService(this);
    }

    /**
     * Attempts to log in to iCloud.
     *
     * @param username the username.
     * @param password the password.
     * @return the map of values returned by iCloud.
     */
    public Map<String, Object> authenticate(@Nonnull String username, @Nonnull char[] password)
    {
        Map<String, Object> params = ImmutableMap.of(
            "apple_id", username,
            "password", new String(password),
            "extended_login", false);

        return authenticate(params);
    }

    /**
     * Attempts to log in to iCloud.
     *
     * @param params the map of parameters to pass to login.
     * @return the map of values returned by iCloud.
     */
    public Map<String, Object> authenticate(Map<String, Object> params)
    {
        try
        {
            URIBuilder uriBuilder = new URIBuilder(setupEndPoint + "/login");
            populateUriParameters(uriBuilder);
            URI uri = uriBuilder.build();

            HttpPost post = new HttpPost(uri);
            post.setEntity(new StringEntity(new Gson().toJson(params), StandardCharsets.UTF_8));
            populateRequestHeadersParameters(post);

            Map<String, Object> result = httpClient.execute(post, new JsonToMapResponseHandler());
            if (result == null)
            {
                throw new RuntimeException("Failed to log into iCloud");
            }

            if (Boolean.FALSE.equals(result.get("success")))
            {
                throw new RuntimeException("Failed to log into iCloud: " + result.get("error"));
            }

            loginInfo = result;

            // Grab the session ID
            Map<String, Object> dsInfoMap = (Map<String, Object>) result.get("dsInfo");
            dsid = (String) dsInfoMap.get("dsid");

            return loginInfo;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks whether two-factor authentication is enabled for this account.
     *
     * @return {@code true} if two-factor authentication is enabled.
     */
    public boolean isTwoFactorEnabled()
    {
        return Boolean.TRUE.equals(loginInfo.get("hsaChallengeRequired"));
    }

    /**
     * Gets the trusted two-factor authentication devices for the current account.
     *
     * @return the list of trusted devices.
     */
    public List<TrustedDevice> getTrustedDevices()
    {
        try
        {
            URIBuilder uriBuilder = new URIBuilder(setupEndPoint + "/listDevices");
            populateUriParameters(uriBuilder);
            URI uri = uriBuilder.build();

            HttpGet httpGet = new HttpGet(uri);
            populateRequestHeadersParameters(httpGet);

            String result = httpClient.execute(httpGet, new StringResponseHandler());
            if (result == null)
            {
                throw new RuntimeException("Failed to get trusted devices");
            }

            TrustedDevices trustedDevices = new Gson().fromJson(result, TrustedDevices.class);

            return Arrays.asList(trustedDevices.devices);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>Requests that a two-factor verification code be sent to the given trusted device. The value sent to the device
     * should be submitted to {@link #validateManualVerificationCode(TrustedDevice, String, char[])} for verification.</p>
     *
     * <p>Note: newer devices will automatically display a verification code without manually requesting one, and that
     * must be submitted via {@link }.</p>
     *
     * @param device the device to send the verification code to.
     */
    public void sendManualVerificationCode(TrustedDevice device)
    {
        try
        {
            URIBuilder uriBuilder = new URIBuilder(setupEndPoint + "/sendVerificationCode");
            populateUriParameters(uriBuilder);
            URI uri = uriBuilder.build();

            HttpPost post = new HttpPost(uri);
            post.setEntity(new StringEntity(new Gson().toJson(device), StandardCharsets.UTF_8));
            populateRequestHeadersParameters(post);

            Map<String, Object> response = httpClient.execute(post, new JsonToMapResponseHandler());

            if (!Boolean.TRUE.equals(response.get("success")))
            {
                throw new IllegalStateException("Failed to send verification code: " + response.get("errorMessage"));
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Validates the manually requested verification code. See {@link #sendManualVerificationCode(TrustedDevice)}.
     *
     * @param device   the device the code was sent to.
     * @param code     the code.
     * @param password the user's password.
     */
    public void validateManualVerificationCode(TrustedDevice device, String code, char[] password)
    {
        try
        {
            URIBuilder uriBuilder = new URIBuilder(setupEndPoint + "/validateManualVerificationCode");
            populateUriParameters(uriBuilder);
            URI uri = uriBuilder.build();

            TrustedDeviceResponse responseDevice = new TrustedDeviceResponse();
            responseDevice.areaCode = device.areaCode;
            responseDevice.deviceType = device.deviceType;
            responseDevice.deviceId = device.deviceId;
            responseDevice.phoneNumber = device.phoneNumber;
            responseDevice.verificationCode = code;
            responseDevice.trustBrowser = true;

            HttpPost post = new HttpPost(uri);
            post.setEntity(new StringEntity(new Gson().toJson(responseDevice), StandardCharsets.UTF_8));
            populateRequestHeadersParameters(post);

            Map<String, Object> response = httpClient.execute(post, new JsonToMapResponseHandler());

            if (!Boolean.TRUE.equals(response.get("success")))
            {
                if (Double.valueOf(-21669.0).equals(response.get("errorCode")))
                {
                    throw new RuntimeException("Invalid verification code");
                } else
                {
                    throw new IllegalStateException("Failed to verify code: " + response.get("errorMessage"));
                }
            }

            // Re-authenticate, which will both update the two-factor authentication data, and ensure that we save the
            // X-APPLE-WEBAUTH-HSA-TRUST cookie
            Map<String, Object> dsInfo = (Map<String, Object>) loginInfo.get("dsInfo");
            authenticate((String) dsInfo.get("appleId"), password);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the iCloud storage usage.
     *
     * @return the map of storage usage details.
     */
    public Map<String, Object> getStorageUsage()
    {
        try
        {
            URIBuilder uriBuilder = new URIBuilder(setupEndPoint + "/storageUsageInfo");
            populateUriParameters(uriBuilder);
            URI uri = uriBuilder.build();

            HttpPost post = new HttpPost(uri);
            populateRequestHeadersParameters(post);

            Map<String, Object> result = httpClient.execute(post, new JsonToMapResponseHandler());
            if (result == null)
            {
                throw new RuntimeException("Failed to get storage usage info");
            }

            if (Boolean.FALSE.equals(result.get("success")))
            {
                throw new RuntimeException("Failed to get storage usage info: " + result.get("error"));
            }

            return result;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the login info.
     *
     * @return the login info.
     */
    public Map<String, Object> getLoginInfo()
    {
        return loginInfo;
    }

    /**
     * Gets the web services map.
     *
     * @return the web services map.
     */
    public Map<String, Object> getWebServicesMap()
    {
        //noinspection unchecked
        return (Map<String, Object>) loginInfo.get("webservices");
    }

    /**
     * Gets the HTTP client.
     *
     * @return the client.
     */
    public CloseableHttpClient getHttpClient()
    {
        return httpClient;
    }

    /**
     * Gets the cookie store.
     *
     * @return the store.
     */
    public CookieStore getCookieStore()
    {
        return cookieStore;
    }

    /**
     * Gets the client ID.
     *
     * @return the client ID.
     */
    public String getClientId()
    {
        return clientId;
    }

    /**
     * Gets the 'idmsa' service.
     *
     * @return the service.
     */
    public IdmsaService getIdmsaService()
    {
        return idmsaService;
    }

    /**
     * Populates the URI parameters for a request.
     *
     * @param uriBuilder the URI builder.
     */
    public void populateUriParameters(URIBuilder uriBuilder)
    {
        uriBuilder
            .addParameter("clientId", clientId)
            .addParameter("clientBuildNumber", "14E45");

        if (!Strings.isNullOrEmpty(dsid))
        {
            uriBuilder.addParameter("dsid", dsid);
        }
    }

    /**
     * Gets the session ID.
     *
     * @return the session ID.
     */
    public String getSessionId()
    {
        return dsid;
    }

    /**
     * Populates the HTTP request headers.
     *
     * @param request the request to populate.
     */
    public void populateRequestHeadersParameters(HttpUriRequestBase request)
    {
        request.setHeader("Origin", endPoint);
        request.setHeader("Referer", endPoint + "/");
        request.setHeader("User-Agent", "Opera/9.52 (X11; Linux i686; U; en)");
    }

    @Override
    public void close() throws IOException
    {
        httpClient.close();
    }
}

