/*
 * Copyright 2019 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.bootstrap.config;

import com.navercorp.pinpoint.bootstrap.BootLogger;
import com.navercorp.pinpoint.common.util.PropertyUtils;
import com.navercorp.pinpoint.common.util.SimpleProperty;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * @author Woonduk Kang(emeroad)
 */
class SimplePropertyLoader implements PropertyLoader {

//    private static final String SEPARATOR = File.separator;

    private final BootLogger logger = BootLogger.getLogger(this.getClass());
    private final SimpleProperty systemProperty;

    private final Path agentRootPath;
    private final Path profilesPath;


    public SimplePropertyLoader(SimpleProperty systemProperty, Path agentRootPath, Path profilesPath) {
        this.systemProperty = Objects.requireNonNull(systemProperty, "systemProperty");
        this.agentRootPath = Objects.requireNonNull(agentRootPath, "agentRootPath");
        this.profilesPath = profilesPath;
    }

    @Override
    public Properties load() {
        final Path defaultConfigPath = this.agentRootPath.resolve(Profiles.CONFIG_FILE_NAME);

        final Properties defaultProperties = new Properties();

        final String externalConfig = this.systemProperty.getProperty(Profiles.EXTERNAL_CONFIG_KEY);
        if (externalConfig != null) {
            logger.info(String.format("load external config:%s", externalConfig));
            loadFileProperties(defaultProperties, Paths.get(externalConfig));
        } else {
            logger.info(String.format("load default config:%s", defaultConfigPath));
            loadFileProperties(defaultProperties, defaultConfigPath);
        }
        loadSystemProperties(defaultProperties);
        saveLogConfigLocation(defaultProperties);
        return defaultProperties;
    }


    private void saveLogConfigLocation(Properties properties) {
        String activeProfile = systemProperty.getProperty(Profiles.ACTIVE_PROFILE_KEY);
        if (activeProfile == null) {
            throw new RuntimeException("Failed to read " + Profiles.ACTIVE_PROFILE_KEY + " from systemProperty");
        }

        LogConfigResolver logConfigResolver = new ProfileLogConfigResolver(profilesPath, activeProfile);
        final Path log4jLocation = logConfigResolver.getLogPath();

        properties.put(Profiles.LOG_CONFIG_LOCATION_KEY, log4jLocation.toString());
        logger.info(String.format("logConfig path:%s", log4jLocation));
    }



    private void loadFileProperties(Properties properties, Path filePath) {
        try {
            PropertyUtils.loadProperty(properties, filePath);
        } catch (IOException e) {
            logger.info(String.format("%s load fail Caused by:%s", filePath, e.getMessage()));
            throw new IllegalStateException(String.format("%s load fail Caused by:%s", filePath, e.getMessage()));
        }
    }

    private void loadSystemProperties(Properties dstProperties) {
        Set<String> stringPropertyNames = this.systemProperty.stringPropertyNames();
        for (String propertyName : stringPropertyNames) {
            boolean isPinpointProperty = propertyName.startsWith("bytecode.") || propertyName.startsWith("profiler.") || propertyName.startsWith("pinpoint.");
            if (isPinpointProperty) {
                String val = this.systemProperty.getProperty(propertyName);
                dstProperties.setProperty(propertyName, val);
            }
        }
    }

}
