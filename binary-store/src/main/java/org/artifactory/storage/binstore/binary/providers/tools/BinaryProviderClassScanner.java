package org.artifactory.storage.binstore.binary.providers.tools;

import com.google.common.collect.Maps;
import org.artifactory.storage.binstore.annotation.BinaryProviderClassInfo;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Map;

/**
 * @author gidis
 */
public class BinaryProviderClassScanner {
    public static Map<String, Class> loadProvidersMap() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(BinaryProviderClassInfo.class));
        Map<String, Class> providersMap = Maps.newHashMap();
        for (BeanDefinition bd : scanner.findCandidateComponents("org.artifactory.storage.db.binstore")) {
            updateMap(providersMap, bd);
        }
        for (BeanDefinition bd : scanner.findCandidateComponents("org.artifactory.addon.filestore")) {
            updateMap(providersMap, bd);
        }
        for (BeanDefinition bd : scanner.findCandidateComponents("org.artifactory.storage.binstore")) {
            updateMap(providersMap, bd);
        }
        for (BeanDefinition bd : scanner.findCandidateComponents("org.artifactory.addon.filestore.*")) {
            updateMap(providersMap, bd);
        }

        return providersMap;
    }

    private static void updateMap(Map<String, Class> providersMap, BeanDefinition bd) {
        try {
            String beanClassName = bd.getBeanClassName();
            Class<?> beanClass = Class.forName(beanClassName);
            BinaryProviderClassInfo annotation = beanClass.getAnnotation(BinaryProviderClassInfo.class);
            providersMap.put(annotation.nativeName(), beanClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to init Binary provider. Reason class not found.", e);
        }
    }
}
