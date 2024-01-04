package com.github.vatbub.common.core;

/*-
 * #%L
 * FOKProjects Common Core
 * %%
 * Copyright (C) 2016 - 2018 Frederik Kammel
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


import android.content.Context;
import com.google.common.hash.Hasher;

import java.io.File;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public abstract class Common {
    public static final String UNKNOWN_APP_VERSION = "unknown version";
    public static final String UNKNOWN_BUILD_NUMBER = "unknown build number";
    private static Common instance;

    public static void useDefaultImplementation() {
//        useImplementation(new CommonAndroidImpl());
    }

    public static void useAndroidImplementation(Context context){
        useImplementation(new CommonAndroidImpl(context));
    }

    public static void useImplementation(Common implementation){
        instance = implementation;
    }

    public static synchronized Common getInstance() {
        if (instance == null) {
//            useDefaultImplementation();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    // General

    /**
     * Returns the current time as a String.
     *
     * @return The current time in the format "yyyy-MM-dd_HH-mm-ss"
     */
    public abstract String getCurrentTimeStamp();

    /**
     * Returns the time when the app was launched as a String.
     *
     * @return The time when the app was launched in the format
     * "yyyy-MM-dd_HH-mm-ss"
     */
    public abstract String getLaunchTimeStamp();

    /**
     * Gets the appData directory of the os including a subdirectory for this
     * app.<br>
     * In case the current OS is Windows, it returns
     * {@code C:\Users\(username)\AppData\Roaming\(appName)\}, in case of Mac,
     * it returns
     * {@code (home directory)/Library/Application Support/(appName)/} and in
     * case of Linux, it returns the home directory.<br>
     * <br>
     * The path returned by this method always finishes with a \ (Windows) or a
     * / (Mac, Linux) (See {@code File#seperator})<br>
     * As this method uses the app name, you need to set the app name using
     * {@link Common#setAppName(String)} or you will get a
     * {@code NullPointerException}<br>
     * <br>
     *
     * @return The sub directory of the home directory of the os where the app
     * can save all files that need to persist, e. g. settings
     * @throws NullPointerException if no app name is specified using
     *                              {@link Common#setAppName(String)}
     * @see Common#setAppName(String)
     * @see Common#getAndCreateAppDataPath()
     */
    public abstract String getAppDataPath();

    /**
     * Same as {@link #getAppDataPath()} but converts its output into a {@code File} object.
     *
     * @return Same as {@link #getAppDataPath()} but converts its output into a {@code File} object.
     */
    public abstract File getAppDataPathAsFile();

    /**
     * Returns the same path as {@link Common#getAppDataPath()} but also creates
     * the whole underlying folder structure if not already present.
     *
     * @return The sub directory of the home directory of the os where the app
     * can save all files that need to persist, e. g. settings
     * @throws NullPointerException if no app name is specified using
     *                              {@link Common#setAppName(String)}
     * @see Common#setAppName(String)
     * @see Common#getAppDataPath()
     */
    public abstract String getAndCreateAppDataPath();

    /**
     * Same as {@link #getAndCreateAppDataPath()} but converts its output into a {@code File} object.
     *
     * @return Same as {@link #getAndCreateAppDataPath()} but converts its output into a {@code File} object.
     */
    public abstract File getAndCreateAppDataPathAsFile();

    /**
     * Get the current name of the app that was set with
     * {@link #setAppName(String)}.
     *
     * @return The current name of the app that was set with
     * {@link #setAppName(String)}.
     */
    public abstract String getAppName();

    /**
     * Sets the name of the app. This is necessary to get the AppDataPath using
     * {@link #getAppDataPath()}.
     *
     * @param appName The name of the app. Must not be an empty string!
     */
    public abstract void setAppName(String appName);

    /**
     * Returns the current value to be used as a mock app version.
     *
     * @return The current mock app version value or "" if no mock app version
     * is in use.
     */
    public abstract String getMockAppVersion();

    /**
     * Enables a mock app version. If a mock app version is set,
     * {@link #getAppVersion()} will not return the mock app version instead of
     * the actual version.
     *
     * @param version The version string to be used as the mock app version
     */
    public abstract void setMockAppVersion(String version);

    /**
     * Clears a mock app version set using {@link #setMockAppVersion(String)}
     * and replaces it with the actual app version.
     */
    public abstract void clearMockAppVersion();

    /**
     * Returns the current value to be used as a mock build number.
     *
     * @return The current mock build number value or "" if no mock build number
     * is in use.
     */
    public abstract String getMockBuildNumber();

    /**
     * Enables a mock build number. If a mock build number is set,
     * {@link #getAppVersion()} will not return the mock build number instead of
     * the actual build number.
     *
     * @param buildNumber The build number to be used as the mock build number
     * @see #clearMockBuildNumber()
     */
    public abstract void setMockBuildNumber(String buildNumber);

    /**
     * Clears a mock build number set using {@link #setMockBuildNumber(String)}
     * and replaces it with the actual build number.
     */
    public abstract void clearMockBuildNumber();

    public abstract String getMockPackaging();

    public abstract void setMockPackaging(String packaging);

    public abstract boolean isMockAppVersionInUse();

    public abstract boolean isMockBuildNumberInUse();

    public abstract boolean isMockPackagingInUse();

    public abstract void clearMockPackaging();

    /**
     * Returns the current artifact version. If a mock app version is specified,
     * the mock app version is returned and not the actual app version.
     *
     * @return The current artifact version or the String
     * {@link #UNKNOWN_APP_VERSION} if the version cannot be determined
     * or the mock app version if one is specified.
     */
    public abstract String getAppVersion();

    /**
     * Returns the current artifact build number. If a mock build number is
     * specified, the mock build number is returned and not the actual build
     * number.<br>
     * <br>
     * For this method to work, the build number must be saved in the
     * MANIFEST.MF file in the attribute "Custom-Implementation-Build" (if you
     * didn't specify anything else in
     * {@link #setBuildNumberManifestEntry(String)}).
     *
     * @return The current artifact build number or the String
     * {@link #UNKNOWN_BUILD_NUMBER} if the build number cannot be
     * determined or the mock build number if one is specified.
     */
    public abstract String getBuildNumber();

    /**
     * Returns the current value of the attribute name where the app looks for
     * its build number when using {@link #getBuildNumber()}.
     *
     * @return The current value of the attribute name where the app looks for
     * its build number when using {@link #getBuildNumber()}.
     */
    public abstract String getBuildNumberManifestEntry();

    /**
     * Changes the Manifest entry where the app looks for its build number when
     * using {@link #getBuildNumber()}. The default value (if you never call
     * this method) is {@code "Custom-Implementation-Build"}
     *
     * @param buildNumberManifestEntry The new entry to use.
     */
    public abstract void setBuildNumberManifestEntry(String buildNumberManifestEntry);

    /**
     * Returns the Path and name of the jar file this app was launched from.
     *
     * @return The Path and name of the jar file this app was launched from.
     */
    public abstract String getPathAndNameOfCurrentJar();

    /**
     * Returns how this java program is packaged. To be more specific, the file
     * extension of this program is returned. If this program is packaged as a
     * *.jar file, this method will return {@code "jar"}. If it is packaged as a
     * Windows Executable, this method will return {@code "exe"} and so on.
     *
     * @return The file extension of this program or {@code null} if the
     * packaging cannot be determined.
     */
    public abstract String getPackaging();

    /**
     * Returns a list of {@code Locale}s that are supported by the specified
     * {@code ResourceBundle}.<br>
     * <b><i>Important Note:</i></b> This method will not check for the default
     * bundle file {@code <resourceBaseName>.properties}.
     *
     * @param bundle The bundle to be checked
     * @return A set of locales supported by the specified resource bundle
     * @deprecated Doesn't work in JDK 14 due to a bug in the JRE
     */
    @Deprecated
    public abstract List<Locale> getLanguagesSupportedByResourceBundle(ResourceBundle bundle);

    /**
     * Returns a list of {@code Locale}s that are supported by the
     * {@code ResourceBundle} with the given base name.<br>
     * <b><i>Important Note:</i></b> This method will not check for the default
     * bundle file {@code <resourceBaseName>.properties}.
     *
     * @param resourceBaseName The base name of the resource bundle
     * @return A set of locales supported by the specified resource bundle
     */
    public abstract List<Locale> getLanguagesSupportedByResourceBundle(String resourceBaseName);

    /**
     * Same as {@link #getUniqueDeviceIdentifierAsDec()} but uses the murmur3_32 hashing algorithm to get a hash that fits into an int variable.
     *
     * @return The unique device identifier converted to an int
     */
    public abstract long getUniqueDeviceIdentifierAsDecLong();

    public abstract Hasher get32bitHasher();

    /**
     * Same as {@link #getUniqueDeviceIdentifier()} but converts the resulting hash into a decimal number.
     *
     * @return The unique device identifier converted to a decimal number
     */
    public abstract BigInteger getUniqueDeviceIdentifierAsDec();

    /**
     * Same as {@link #getUniqueDeviceIdentifier(Hasher)} but converts the resulting hash into a decimal number
     *
     * @param hasher The hasher object to use to hash the hardware properties. IMPORTANT: It is recommended to create a new hasher instance that has not been used prior to this method call to ensure consistency across calls to this method.
     * @return The unique device identifier converted to a decimal number
     */
    public abstract BigInteger getUniqueDeviceIdentifierAsDec(Hasher hasher);

    public abstract String getUniqueDeviceIdentifier();

    /**
     * Calculates a unique device identifier.
     * The identifier is created by hashing the following hardware properties:
     * <ul>
     * <li>Operating system:
     * <ul>
     * <li>Family</li>
     * <li>Major version</li>
     * </ul>
     * </li>
     * <li>Drives (All internal drives are used that have a serial number):
     * <ul>
     * <li>Serial number</li>
     * </ul></li>
     * <li>CPU:<ul>
     * <li>Core-count</li>
     * </ul></li>
     * <li>Motherboard:<ul>
     * <li>Serial number</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @param hasher The hasher object to use to hash the hardware properties. IMPORTANT: It is recommended to create a new hasher instance that has not been used prior to this method call to ensure consistency across calls to this method.
     * @return The unique hardware identifier
     */
    public abstract String getUniqueDeviceIdentifier(Hasher hasher);

    /**
     * Determines whether the specified drive is a removable drive or not.
     * This is determined by comparing the drive's model to the name of each connected usb device.
     * A drive is considered removable if one of the following criteria is met:
     * <ul>
     * <li>The drive's model is equal to the name of a usb device</li>
     * <li>The drive's model contains the name of a usb device</li>
     * <li>The name of a usb device contains the drive's model</li>
     * <li>The <a href="https://en.wikipedia.org/wiki/Jaccard_index">Jaccard similarity</a> between the drive's model and a usb device's name is higher than 0.7</li>
     * </ul>
     *
     * @param store      The drive to check
     * @param usbDevices The list of currently connected usb devices. Use {@code new SystemInfo().getHardware().getUsbDevices(false)} to retrieve the list.
     * @return {@code true} if the specified drive is removable, {@code false} otherwise
     */



    /**
     * Same as {@link #getAndCreateAppDataPath()} but returns {@code null} if no app name is defined instead of failing with a {@code NullPointerException}
     *
     * @return {@link #getAndCreateAppDataPath()} or {@code null} if no app name has been defined using {@link #setAppName(String)}
     */
    public abstract String tryGetAndCreateAppDataPath();

    /**
     * Same as {@link #getAppDataPath()} but returns {@code null} if no app name is defined instead of failing with a {@code NullPointerException}
     *
     * @return {@link #getAppDataPath()} or {@code null} if no app name has been defined using {@link #setAppName(String)}
     */
    public abstract String tryGetAppDataPath();

    /**
     * Same as {@link #getAppDataPathAsFile()} but returns {@code null} if no app name is defined instead of failing with a {@code NullPointerException}
     *
     * @return {@link #getAppDataPathAsFile()} or {@code null} if no app name has been defined using {@link #setAppName(String)}
     */
    public abstract File tryGetAppDataPathAsFile();

    /**
     * Same as {@link #getAndCreateAppDataPathAsFile()} but returns {@code null} if no app name is defined instead of failing with a {@code NullPointerException}
     *
     * @return {@link #getAndCreateAppDataPathAsFile()} or {@code null} if no app name has been defined using {@link #setAppName(String)}
     */
    public abstract File tryGetAndCreateAppDataPathAsFile();
}
