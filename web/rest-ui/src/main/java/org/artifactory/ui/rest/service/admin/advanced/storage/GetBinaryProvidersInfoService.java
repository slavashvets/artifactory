package org.artifactory.ui.rest.service.admin.advanced.storage;

import org.artifactory.api.jackson.JacksonFactory;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.storage.StorageService;
import org.jfrog.storage.binstore.ifc.model.BinaryTreeElement;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * @author Gidi Shabat
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetBinaryProvidersInfoService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(GetBinaryProvidersInfoService.class);

    @Autowired
    private StorageService storageService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        BinaryTreeElement<Map<String, String>> binaryProviderInfo = storageService.getBinaryProviderInfo();
        // Serialize the result to Json
        ObjectMapper objectMapper = JacksonFactory.createObjectMapper();
        String info = null;
        try {
            info = objectMapper.writeValueAsString(binaryProviderInfo);
        } catch (IOException e) {
            String message = "Failed to serialize the binary provider info to JSON";
            log.error(message, e);
            response.error(message);
        }
        //noinspection unchecked
        response.iModel(info);
    }


}
