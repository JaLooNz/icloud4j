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

import com.github.tmyroadctfig.icloud4j.json.DriveNodeDetails;
import com.github.tmyroadctfig.icloud4j.util.ICloudUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Access to the iCloud Drive service.
 */
public class DriveService
{

    private final ICloudService iCloudService;
    private final String serviceRoot;
    private final String docsServiceRoot;

    public DriveService(ICloudService iCloudService)
    {
        this.iCloudService = iCloudService;

        Map<String, Object> driveSettings = (Map<String, Object>) iCloudService.getWebServicesMap().get("drivews");
        serviceRoot = (String) driveSettings.get("url");

        Map<String, Object> docsSettings = (Map<String, Object>) iCloudService.getWebServicesMap().get("docws");
        docsServiceRoot = (String) docsSettings.get("url");
    }

    public DriveNode getRoot()
    {
        String rootId = "FOLDER::com.apple.CloudDocs::root";
        return new DriveNode(iCloudService, this, rootId, getNodeDetails(rootId));
    }

    public DriveNodeDetails getNodeDetails(String nodeId)
    {
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

        if (detailsArray != null && detailsArray.length > 0)
        {
            return detailsArray[0];
        } else
        {
            throw new RuntimeException("Empty response from iCloud Drive service.");
        }
    }

    public List<DriveNode> getChildren(String parentId)
    {
        DriveNodeDetails nodeDetails = getNodeDetails(parentId);
        if (nodeDetails.items == null)
        {
            return Collections.emptyList();
        }

        return Stream.of(nodeDetails.items)
            .map(childDetails -> new DriveNode(iCloudService, this, childDetails.drivewsid, childDetails))
            .collect(Collectors.toList());
    }

    public String getServiceUrl()
    {
        return serviceRoot;
    }

    public String getDocsServiceUrl()
    {
        return docsServiceRoot;
    }
}

