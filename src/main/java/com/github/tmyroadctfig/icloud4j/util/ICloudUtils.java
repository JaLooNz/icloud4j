package com.github.tmyroadctfig.icloud4j.util;

import com.github.tmyroadctfig.icloud4j.ICloudException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * iCloud utilities.
 */
public class ICloudUtils {

    private static final Gson gson = new Gson();

    /**
     * Parses a JSON response from a POST request.
     */
    public static <T> T parseJsonResponse(CloseableHttpClient httpClient, HttpPost post, Class<T> responseClass) {
        try {
            return httpClient.execute(post, (ClassicHttpResponse response) -> {
                String rawResponseContent = new StringResponseHandler().handleResponse(response);

                try {
                    return gson.fromJson(rawResponseContent, responseClass);
                } catch (JsonSyntaxException e) {
                    Map<String, Object> errorMap = gson.fromJson(rawResponseContent, Map.class);
                    throw new ICloudException(response, errorMap);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Parses a JSON response from a GET request.
     */
    public static <T> T parseJsonResponse(CloseableHttpClient httpClient, HttpGet get, Class<T> responseClass) {
        try {
            return httpClient.execute(get, (ClassicHttpResponse response) -> {
                String rawResponseContent = new StringResponseHandler().handleResponse(response);

                try {
                    return gson.fromJson(rawResponseContent, responseClass);
                } catch (JsonSyntaxException e) {
                    Map<String, Object> errorMap = gson.fromJson(rawResponseContent, Map.class);
                    throw new ICloudException(response, errorMap);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
