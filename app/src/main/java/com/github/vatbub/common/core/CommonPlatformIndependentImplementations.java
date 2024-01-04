package com.github.vatbub.common.core;

/*-
 * #%L
 * FOKProjects Common
 * %%
 * Copyright (C) 2016 Frederik Kammel
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import com.github.vatbub.common.core.logging.FOKLogger;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.io.File;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Implements all methods of {@link Common} that do not require platform dependent implementations
 */
public abstract class CommonPlatformIndependentImplementations extends Common {
    private final Date launchDate = new Date();
    private String appName;
    private String mockAppVersion;
    private String mockBuildNumber;
    private String mockPackaging;
    private String awsAccessKey;
    private String awsSecretAccessKey;
    private String buildNumberManifestEntry = "Custom-Implementation-Build";

    protected CommonPlatformIndependentImplementations() {
    }

    // General
    @Override
    public String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");// dd/MM/yyyy
        Date now = new Date();
        return sdfDate.format(now);
    }

    @Override
    public String getLaunchTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");// dd/MM/yyyy
        return sdfDate.format(launchDate);
    }

    @Override
    public File getAppDataPathAsFile() {
        return new File(getAppDataPath());
    }

    @Override
    public String getAndCreateAppDataPath() {
        return getAndCreateAppDataPathAsFile().getPath() + File.separator;
    }

    @Override
    public File getAndCreateAppDataPathAsFile() {
        File path = getAppDataPathAsFile();
        if (!path.exists() && !path.mkdirs())
            throw new IllegalStateException("Unable to create the app data path: " + path.getAbsolutePath());
        return path;
    }

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public void setAppName(String appName) {
        if (appName != null && appName.isEmpty())
            throw new IllegalArgumentException("The supplied app name must not be empty");

        this.appName = appName;
    }

    @Override
    public String getMockAppVersion() {
        return mockAppVersion;
    }

    @Override
    public void setMockAppVersion(String version) {
        FOKLogger.info(Common.class.getName(), "Now using mock app version " + version);
        mockAppVersion = version;
    }

    @Override
    public void clearMockAppVersion() {
        setMockAppVersion(null);
    }

    @Override
    public String getMockBuildNumber() {
        return mockBuildNumber;
    }

    @Override
    public void setMockBuildNumber(String buildNumber) {
        FOKLogger.info(Common.class.getName(), "Now using mock build number " + buildNumber);
        mockBuildNumber = buildNumber;
    }

    @Override
    public void clearMockBuildNumber() {
        setMockBuildNumber(null);
    }

    @Override
    public String getMockPackaging() {
        return mockPackaging;
    }

    @Override
    public void setMockPackaging(String packaging) {
        FOKLogger.info(Common.class.getName(), "Now using mock packaging " + packaging);
        mockPackaging = packaging;
    }

    @Override
    public boolean isMockAppVersionInUse() {
        return getMockAppVersion() != null;
    }

    @Override
    public boolean isMockBuildNumberInUse() {
        return getMockBuildNumber() != null;
    }

    @Override
    public boolean isMockPackagingInUse() {
        return getMockPackaging() != null;
    }

    @Override
    public void clearMockPackaging() {
        setMockPackaging(null);
    }

    @Override
    public String getAppVersion() {
        if (isMockAppVersionInUse()) {
            // A mock version was defined
            return mockAppVersion;
        } else {
            String ver = Common.class.getPackage().getImplementationVersion();
            if (ver == null) {
                return UNKNOWN_APP_VERSION;
            } else {
                return ver;
            }
        }
    }

    @Override
    public String getBuildNumber() {
        return mockBuildNumber;

    }

    @Override
    public String getBuildNumberManifestEntry() {
        return buildNumberManifestEntry;
    }

    @Override
    public void setBuildNumberManifestEntry(String buildNumberManifestEntry) {
        this.buildNumberManifestEntry = buildNumberManifestEntry;
    }

    @Override
    @Deprecated
    public List<Locale> getLanguagesSupportedByResourceBundle(ResourceBundle bundle) {
        return getLanguagesSupportedByResourceBundle(bundle.getBaseBundleName());
    }

    @Override
    public List<Locale> getLanguagesSupportedByResourceBundle(String resourceBaseName) {
        List<Locale> locales = new ArrayList<>();

        for (Locale locale : Locale.getAvailableLocales()) {
            try {
                // Try to load the resource bundle with the locale
                ResourceBundle bundle = ResourceBundle.getBundle(resourceBaseName, locale);
                // No fail so add the locale to the list
                if (bundle.getLocale().equals(locale)) {
                    locales.add(locale);
                }
            } catch (MissingResourceException ex) {
                // failed, so don't add it to the list
            }
        }

        return Collections.unmodifiableList(locales);
    }

    @Override
    public long getUniqueDeviceIdentifierAsDecLong() {
        return Long.parseLong(getUniqueDeviceIdentifier(get32bitHasher()), 16);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BigInteger getUniqueDeviceIdentifierAsDec() {
        return getUniqueDeviceIdentifierAsDec(Hashing.md5().newHasher());
    }

    @Override
    public BigInteger getUniqueDeviceIdentifierAsDec(Hasher hasher) {
        return new BigInteger(getUniqueDeviceIdentifier(hasher).trim(), 16);
    }

    @Override
    public String getUniqueDeviceIdentifier() {
        return getUniqueDeviceIdentifier(Hashing.md5().newHasher());
    }


    @Override
    public String tryGetAndCreateAppDataPath() {
        try {
            return getAndCreateAppDataPath();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String tryGetAppDataPath() {
        try {
            return getAppDataPath();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public File tryGetAppDataPathAsFile() {
        try {
            return getAppDataPathAsFile();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public File tryGetAndCreateAppDataPathAsFile() {
        try {
            return getAndCreateAppDataPathAsFile();
        } catch (Exception e) {
            return null;
        }
    }
}
