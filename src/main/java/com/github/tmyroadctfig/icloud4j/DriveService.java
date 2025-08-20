package com.github.tmyroadctfig.icloud4j;

import com.github.tmyroadctfig.icloud4j.json.DriveNodeDetails;
import com.github.tmyroadctfig.icloud4j.util.ICloudUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Access to the iCloud Drive service.
 */
public class DriveService {

    private final ICloudService iCloudService;
    private final String serviceRoot;
    private final String docsServiceRoot;

    public DriveService(ICloudService iCloudService) {
        this.iCloudService = iCloudService;

        Map<String, Object> driveSettings = (Map<String, Object>) iCloudService.getWebServicesMap().get("drivews");
        serviceRoot = (String) driveSettings.get("url");

        Map<String, Object> docsSettings = (Map<String, Object>) iCloudService.getWebServicesMap().get("docws");
        docsServiceRoot = (String) docsSettings.get("url");
    }

    public DriveNode getRoot() {
        String rootId = "FOLDER::com.apple.CloudDocs::root";
        return new DriveNode(iCloudService, this, rootId, getNodeDetails(rootId));
    }

    public DriveNodeDetails getNodeDetails(String nodeId) {
        HttpPost post = new HttpPost(serviceRoot + "/retrieveItemDetailsInFolders");
        iCloudService.populateRequestHeadersParameters(post);
        post.addHeader("clientMasteringNumber", "14E45");
        post.setEntity(new StringEntity(
            String.format("[{\"drivewsid\":\"%s\",\"partialData\":false}]", nodeId),
            ContentType.APPLICATION_JSON
        ));

        DriveNodeDetails[] detailsArray = ICloudUtils.parseJsonResponse(
            iCloudService.getHttpClient(),
            post,
            DriveNodeDetails[].class
        );

        if (detailsArray != null && detailsArray.length > 0) {
            return detailsArray[0];
        } else {
            throw new RuntimeException("Empty response from iCloud Drive service.");
        }
    }

    public List<DriveNode> getChildren(String parentId) {
        DriveNodeDetails nodeDetails = getNodeDetails(parentId);
        if (nodeDetails.items == null) {
            return Collections.emptyList();
        }

        return Stream.of(nodeDetails.items)
            .map(childDetails -> new DriveNode(iCloudService, this, childDetails.drivewsid, childDetails))
            .collect(Collectors.toList());
    }

    public String getServiceUrl() {
        return serviceRoot;
    }

    public String getDocsServiceUrl() {
        return docsServiceRoot;
    }
}
