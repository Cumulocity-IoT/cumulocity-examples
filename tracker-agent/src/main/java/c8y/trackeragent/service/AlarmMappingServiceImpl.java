/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA,
 * and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided
 * for in your License Agreement with Software AG.
 */

package c8y.trackeragent.service;

import java.util.Locale;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import c8y.trackeragent.configuration.ConfigUtils;

@Component
public class AlarmMappingServiceImpl extends ReloadableResourceBundleMessageSource implements AlarmMappingService {

    private static Logger logger = LoggerFactory.getLogger(AlarmMappingServiceImpl.class);

    private static final Locale LOCALE = Locale.GERMANY;
    private static final int CACHE_SECONDS = 600;

    static String ENTRY_TYPE = "type";
    static String ENTRY_TEXT = "text";
    static String ENTRY_SEVERITY = "severity";

    private final String basename;

    public AlarmMappingServiceImpl() {
        this.basename = "file:" + ConfigUtils.get().getConfigDir() + "/alarm-configuration";
    }

    public AlarmMappingServiceImpl(String baseName) {
        this.basename = baseName;
    }

    @Override
    public String getType(String name) {
        return getMessage(name, ENTRY_TYPE);
    }

    @Override
    public String getText(String name, Object... args) {
        return getMessage(name, ENTRY_TEXT, args);
    }

    @Override
    public String getSeverity(String name) {
        return getMessage(name, ENTRY_SEVERITY);
    }

    private String getMessage(String name, String keyPostfix, Object... args) {
        String code = asCode(name, keyPostfix);
        String result = getMessage(code, args, LOCALE);
        return StringUtils.trim(result);
    }

    static String asCode(String name, String keyPostfix) {
        return name.toLowerCase().replaceAll(" ", "_") + "." + keyPostfix;
    }

    @PostConstruct
    public void init() {
        logger.info("Load properties from {}", basename);
        setBasenames(basename);
        setCacheSeconds(CACHE_SECONDS);
        setDefaultEncoding("UTF-8");
        setUseCodeAsDefaultMessage(true);
    }
}
