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

import java.io.*;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;

@SuppressWarnings("WeakerAccess")
public class Config {

    private final Properties onlineProps = new Properties();
    private final Properties offlineProps = new Properties();
    private volatile ConfigSource currentlyActiveSource;
    private boolean offlineMode;

    /**
     * Creates a new {@code Config}-Instance from the specified
     * {@code *.properties}-file
     *
     * @param configFile The {@code *.properties}-file to import.
     * @throws IOException If the specified file does not exist or cannot be read.
     */
    public Config(URL configFile) throws IOException {
        this.readConfigFromFile(configFile);
    }

    /**
     * Creates a new {@code Config}-instance and reads the config from the
     * remote url. If this fails for any reason and a cached config is
     * available, the cached config is used instead. If the remote config can't
     * be read and no cached version is available, the fallbackConfig is
     * used.<br>
     * <br>
     * This constructor caches the remote config for offline use.
     *
     * @param remoteConfig   The {@code URL} of the remote config to be read.
     * @param fallbackConfig The config file to be read in case the {@code remoteConfig}
     *                       cannot be downloaded and no cached version is available.
     *                       downloaded for offline use.
     * @param cacheFileName  The file name of the offline cache.
     * @throws FileNotFoundException If the specified fallbackConfig does not exist.
     * @throws IOException           If the specified fallbackConfig cannot be read.
     */
    public Config(URL remoteConfig, URL fallbackConfig, String cacheFileName) throws IOException {
        this(remoteConfig, fallbackConfig, true, cacheFileName);
    }

    /**
     * Creates a new {@code Config}-instance and reads the config from the
     * remote url asynchronously. See the constructor below to see how it
     * works. If this fails for any reason and a cached config is available, the
     * cached config is used instead. If the remote config can't be read and no
     * cached version is available, the fallbackConfig is used.
     *
     * @param remoteConfig      The {@code URL} of the remote config to be read.
     * @param fallbackConfig    The config file to be read in case the {@code remoteConfig}
     *                          cannot be downloaded and no cached version is available.
     * @param cacheRemoteConfig If {@code true}, the remote config will be cached once
     *                          downloaded for offline use.
     * @param cacheFileName     The file name of the offline cache. Only taken into account if
     *                          {@code cacheRemoteConfig==true}
     * @throws IOException If the specified fallbackConfig does not exist or cannot be read.
     */
    @SuppressWarnings("RedundantThrows")
    public Config(URL remoteConfig, URL fallbackConfig, boolean cacheRemoteConfig, String cacheFileName)
            throws IOException {
        this(remoteConfig, fallbackConfig, cacheRemoteConfig, cacheFileName, false);
    }

    /**
     * Creates a new {@code Config}-instance and reads the config from the
     * remote url. If this fails for any reason and a cached config is
     * available, the cached config is used instead. If the remote config can't
     * be read and no cached version is available, the fallbackConfig is used.
     *
     * @param remoteConfig       The {@code URL} of the remote config to be read.
     * @param fallbackConfig     The config file to be read in case the {@code remoteConfig}
     *                           cannot be downloaded and no cached version is available.
     * @param cacheRemoteConfig  If {@code true}, the remote config will be cached once
     *                           downloaded for offline use.
     * @param cacheFileName      The file name of the offline cache. Only taken into account if
     *                           {@code cacheRemoteConfig==true}
     * @param readAsynchronously If {@code true}, the remote config will be read
     *                           asynchronously. This happens as follows: First, the
     *                           fallbackConfig or the cached config (if one is available) is
     *                           read. Then, the remote config is loaded in a new Thread. This
     *                           ensures that your Config is instantly accessible while it
     *                           seamlessly updates in the background. You do not need to
     *                           refresh the config once the remote config is loaded, it will
     *                           do that by itself. If the remote config fails to load, a
     *                           message will appear in the log and you will use the cached
     *                           config/fallbackConfig.
     * @throws IOException If the specified fallbackConfig does not exist or cannot be read.
     */
    public Config(URL remoteConfig, URL fallbackConfig, boolean cacheRemoteConfig, String cacheFileName,
                  boolean readAsynchronously) throws IOException {
        this(remoteConfig, fallbackConfig, cacheRemoteConfig, cacheFileName, readAsynchronously, false);
    }

    /**
     * Creates a new {@code Config}-instance and reads the config from the
     * remote url. If this fails for any reason and a cached config is
     * available, the cached config is used instead. If the remote config can't
     * be read and no cached version is available, the fallbackConfig is used.
     *
     * @param remoteConfig       The {@code URL} of the remote config to be read.
     * @param fallbackConfig     The config file to be read in case the {@code remoteConfig}
     *                           cannot be downloaded and no cached version is available.
     * @param cacheRemoteConfig  If {@code true}, the remote config will be cached once
     *                           downloaded for offline use.
     * @param cacheFileName      The file name of the offline cache. Only taken into account if
     *                           {@code cacheRemoteConfig==true}
     * @param readAsynchronously If {@code true}, the remote config will be read
     *                           asynchronously. This happens as follows: First, the
     *                           fallbackConfig or the cached config (if one is available) is
     *                           read. Then, the remote config is loaded in a new Thread. This
     *                           ensures that your Config is instantly accessible while it
     *                           seamlessly updates in the background. You do not need to
     *                           refresh the config once the remote config is loaded, it will
     *                           do that by itself. If the remote config fails to load, a
     *                           message will appear in the log and you will use the cached
     *                           config/fallbackConfig.
     * @param offlineMode        If {@code true}, offline mode will be simulated
     * @throws IOException If the specified fallbackConfig does not exist or cannot be read.
     */
    public Config(URL remoteConfig, URL fallbackConfig, boolean cacheRemoteConfig, String cacheFileName,
                  boolean readAsynchronously, boolean offlineMode) throws IOException {
        setOfflineMode(offlineMode);
        if (readAsynchronously) {
            this.readRemoteConfigAsynchronous(remoteConfig, fallbackConfig, cacheRemoteConfig, cacheFileName);
        } else {
            this.readRemoteConfig(remoteConfig, fallbackConfig, cacheRemoteConfig, cacheFileName);
        }
    }

    /**
     * Imports the specified {@code *.properties}-file
     *
     * @param file The {@code *.properties}-file to import.
     * @throws IOException If the specified file does not exist or cannot be read.
     */
    private void readConfigFromFile(URL file) throws IOException {
        FOKLogger.info(Config.class.getName(), "Reading config from local file...");
        InputStream inputStream = file.openStream();
        offlineProps.load(inputStream);
        inputStream.close();
    }

    /**
     * Reads the config from the remote url synchronously. If this fails for any reason and a
     * cached config is available, the cached config is used instead. If the
     * remote config can't be read and no cached version is available, the
     * fallbackConfig is used.
     *
     * @param remoteConfig      The {@code URL} of the remote config to be read.
     * @param fallbackConfig    The config file to be read in case the {@code remoteConfig}
     *                          cannot be downloaded and no cached version is available.
     * @param cacheRemoteConfig If {@code true}, the remote config will be cached once
     *                          downloaded for offline use.
     * @throws IOException If the specified fallbackConfig does not exist or cannot be read.
     */
    private void readRemoteConfig(URL remoteConfig, URL fallbackConfig, boolean cacheRemoteConfig,
                                  String cacheFileName) throws IOException {
        // Check if offline cache exists
        checkForOfflineCacheOrLoadFallback(fallbackConfig, cacheFileName);
        if (!isOfflineMode())
            getRemoteConfig(remoteConfig, cacheRemoteConfig, cacheFileName);
    }

    private void getRemoteConfig(URL remoteConfig, boolean cacheRemoteConfig, String cacheFileName) throws IOException {
        FOKLogger.info(Config.class.getName(), "Trying to read remote config...");
        onlineProps.load(remoteConfig.openStream());
        setCurrentlyActiveSource(ConfigSource.ONLINE);
        FOKLogger.info(Config.class.getName(), "Import of remote config successful.");

        if (cacheRemoteConfig) {
            // Update the offline cache
            FOKLogger.info(Config.class.getName(), "Caching remote config for offline use...");
            File f = new File(Common.getInstance().getAndCreateAppDataPath() + cacheFileName);
            if (!f.getParentFile().exists() && !f.getParentFile().mkdirs())
                throw new IllegalStateException("Unable to create the folders for the cached config");
            FileOutputStream out = new FileOutputStream(f);
            onlineProps.store(out, "Config of app " + Common.getInstance().getAppName());
            out.close();
        }
    }

    private void checkForOfflineCacheOrLoadFallback(URL fallbackConfig, String cacheFileName)
            throws IOException {
        File cacheFile = new File(Common.getInstance().getAndCreateAppDataPath() + cacheFileName);
        if (cacheFile.exists()) {
            FOKLogger.info(Config.class.getName(), "Reading cached config...");
            this.readConfigFromFile(cacheFile.toURI().toURL());
            setCurrentlyActiveSource(ConfigSource.CACHE);
        } else {
            FOKLogger.info(Config.class.getName(), "Reading fallbackConfig...");
            this.readConfigFromFile(fallbackConfig);
            setCurrentlyActiveSource(ConfigSource.OFFLINE);
        }
    }

    private void readRemoteConfigAsynchronous(URL remoteConfig, URL fallbackConfig, boolean cacheRemoteConfig,
                                              String cacheFileName) throws IOException {
        // Read offline cache or fallback first
        checkForOfflineCacheOrLoadFallback(fallbackConfig, cacheFileName);

        if (!isOfflineMode()) {
            // Now load the remote config in a new Thread
            Thread loadConfigThread = new Thread(() -> {
                try {
                    getRemoteConfig(remoteConfig, cacheRemoteConfig, cacheFileName);
                } catch (IOException e) {
                    FOKLogger.log(Config.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
                }
            });

            loadConfigThread.setName("loadConfigThread");
            loadConfigThread.start();
        }
    }

    /**
     * Use {@link #getCurrentlyActiveSource()} instead
     * @return {@code true} if the remote config is currently enabled.
     *
     * @deprecated
     */
    @Deprecated
    public boolean isRemoteConfigEnabled() {
        return getCurrentlyActiveSource().equals(ConfigSource.ONLINE);
    }

    /**
     * Checks if the specified key is defined in this Config file.
     *
     * @param key The key of the property to be checked.
     * @return {@code true} if a property with the specified key is found,
     * {@code false} otherwise.
     */
    public boolean contains(String key) {
        return onlineProps.getProperty(key) != null || offlineProps.getProperty(key) != null;
    }

    /**
     * Returns the config value for the specified key or {@code null} if the key
     * was not found. If the key is defined in both, the remote and the fallback config,
     * the value from the remote config will be returned. If the value is <i>only</i> defined
     * in the fallback config but <i>not</i> in the remote config, the local fallback value will
     * be returned. (I. e. the remote and fallback config are merged and the remote config is given priority).
     *
     * @param key The key of the config parameter.
     * @return the config value for the specified key or {@code null} if the key
     * was not found.
     */
    public String getValue(String key) {
        if (onlineProps.containsKey(key))
            return onlineProps.getProperty(key);
        else
            return offlineProps.getProperty(key);
    }

    public boolean isOfflineMode() {
        return offlineMode;
    }

    public void setOfflineMode(boolean offlineMode) {
        this.offlineMode = offlineMode;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();

        for (Entry<Object, Object> e : onlineProps.entrySet()) {
            res.append(e.getKey().toString()).append("=").append(e.getValue()).append("\n");
        }

        for (Entry<Object, Object> e : offlineProps.entrySet()) {
            res.append(e.getKey().toString()).append("=").append(e.getValue()).append("\n");
        }

        return res.toString();
    }

    public ConfigSource getCurrentlyActiveSource() {
        return currentlyActiveSource;
    }

    private void setCurrentlyActiveSource(ConfigSource currentlyActiveSource) {
        this.currentlyActiveSource = currentlyActiveSource;
    }

    public enum ConfigSource {
        ONLINE, CACHE, OFFLINE
    }
}
