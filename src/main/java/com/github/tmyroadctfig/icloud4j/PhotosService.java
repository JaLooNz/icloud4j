package com.github.tmyroadctfig.icloud4j;

import com.github.tmyroadctfig.icloud4j.json.PhotosAlbumsResponse;
import com.github.tmyroadctfig.icloud4j.json.PhotosFolder;
import com.github.tmyroadctfig.icloud4j.util.StringResponseHandler;
import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.net.URIBuilder;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Access to the iCloud Photos service.
 */
public class PhotosService {

    private final ICloudService iCloudService;
    private final String serviceRoot;
    private final String endPoint;
    private final String syncToken;

    public PhotosService(ICloudService iCloudService) {
        this.iCloudService = iCloudService;
        Map<String, Object> photosSettings = (Map<String, Object>) iCloudService.getWebServicesMap().get("photos");
        serviceRoot = (String) photosSettings.get("url");

        endPoint = serviceRoot + "/ph";

        syncToken = getSyncToken();
    }

    private String getSyncToken() {
        try {
            URIBuilder uriBuilder = new URIBuilder(endPoint + "/startup");
            iCloudService.populateUriParameters(uriBuilder);
            HttpGet httpGet = new HttpGet(uriBuilder.build());
            iCloudService.populateRequestHeadersParameters(httpGet);

            HttpClientResponseHandler<String> handler = new StringResponseHandler();
            String rawResponse = iCloudService.getHttpClient().execute(httpGet, handler);

            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, Object> responseMap = new Gson().fromJson(rawResponse, type);

            return (String) responseMap.get("syncToken");
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void populateUriParameters(URIBuilder uriBuilder) {
        uriBuilder
            .addParameter("dsid", iCloudService.getSessionId())
            .addParameter("clientBuildNumber", "14E45")
            .addParameter("clientInstanceId", iCloudService.getClientId())
            .addParameter("syncToken", syncToken);
    }

    public PhotosFolder getAllPhotosAlbum() {
        return getAlbums()
            .stream()
            .filter(folder -> "all-photos".equals(folder.serverId))
            .findFirst()
            .orElse(null);
    }

    public List<PhotosFolder> getAlbums() {
        try {
            URIBuilder uriBuilder = new URIBuilder(endPoint + "/folders");
            populateUriParameters(uriBuilder);
            HttpGet httpGet = new HttpGet(uriBuilder.build());
            iCloudService.populateRequestHeadersParameters(httpGet);

            HttpClientResponseHandler<String> handler = new StringResponseHandler();
            String rawResponse = iCloudService.getHttpClient().execute(httpGet, handler);

            PhotosAlbumsResponse photosAlbumsResponse = new Gson().fromJson(rawResponse, PhotosAlbumsResponse.class);

            return Arrays.stream(photosAlbumsResponse.folders)
                .filter(folder -> "album".equals(folder.type))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
