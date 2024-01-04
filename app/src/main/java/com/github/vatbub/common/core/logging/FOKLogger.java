package com.github.vatbub.common.core.logging;

/*
 * #%L
 * examTrainer
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

import com.github.vatbub.common.core.Common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.logging.*;

/**
 * A frame for all log actions. This class utilizes
 * {@link Logger}
 *
 * @author Frederik Kammel
 */
public class FOKLogger {

    /**
     * A log text that can be used to log generic exceptions. It is recommended to attach the exception to the log entry.
     */
    public static final String DEFAULT_ERROR_TEXT = "An error occurred";
    private static Map<String, FOKLogger> loggerMap = new HashMap<>();
    private static Handler fileHandler;
    private static Handler consoleHandler;
    private static boolean handlersInitialized;
    /**
     * Log messages must have the specified log level or higher to be saved in
     * the log file. {@code Level.ALL} will enable all log levels. Default
     * Value: {@code Level.ALL}
     */
    private static Level fileLogLevel = Level.ALL;
    /**
     * Log messages must have the specified log level or higher to appear on the
     * console. {@code Level.ALL} will enable all log levels. Default Value:
     * {@code Level.ALL}
     */
    private static Level consoleLogLevel = Level.INFO;
    /**
     * The name scheme for the file name of the log. Use the place holder
     * {@code DateTime} for the current date and time.
     */
    private static String logFileName;
    private static String logFilePath;
    private static boolean forceResetLogHandlersOnNextLogAction = false;
    private static boolean fileLoggingDisabled;
    //log uncaught exceptions
    private static final Thread.UncaughtExceptionHandler logUncaughtException = ((thread, throwable) -> {
        String className;
        try {
            className = throwable.getStackTrace()[0].getClassName();
        } catch (Throwable e) {
            FOKLogger.severe(FOKLogger.class.getName(), "Unable to determine class that caused an uncaught exception");
            className = "Unknown class name";
        }
        FOKLogger.log(className, Level.SEVERE, "An uncaught exception occurred in the thread " + thread.getName(), throwable);
    });
    final Logger log;

    /**
     * Creates a new {@link Logger} instance and attaches
     * automatically a {@link FileHandler} and a {@link ConsoleHandler}.<br>
     *
     * @param className The name of the calling class. It is recommended to use the
     *                  fully qualified class name that you can get with
     *                  {@code (YourClassName).class.getName()}.
     */
    private FOKLogger(String className) {
        this(className, combineLogPath(), "log_" + Common.getInstance().getAppName() + "_DateTime.xml");
    }

    /**
     * Creates a new {@link Logger} instance and attaches
     * automatically a {@link FileHandler} and a {@link ConsoleHandler}.
     *
     * @param className      The name of the calling class. It is recommended to use the
     *                       fully qualified class name that you can get with
     *                       {@code (YourClassName).class.getName()}.
     * @param newLogFilePath The file where the log file shall be saved in
     * @param newLogFileName The name of the log file
     */
    public FOKLogger(String className, String newLogFilePath, String newLogFileName) {
        this(className, newLogFilePath, newLogFileName, logFilePath == null && newLogFilePath != null);
    }

    /**
     * Creates a new {@link Logger} instance and attaches
     * automatically a {@link FileHandler} and a {@link ConsoleHandler}.
     *
     * @param className           The name of the calling class. It is recommended to use the
     *                            fully qualified class name that you can get with
     *                            {@code (YourClassName).class.getName()}.
     * @param newLogFilePath      The file where the log file shall be saved in
     * @param newLogFileName      The name of the log file
     * @param forceReloadHandlers If set to {@code true}, the log handlers are reloaded regardless of whether they were already initialized
     */
    public FOKLogger(String className, String newLogFilePath, String newLogFileName, boolean forceReloadHandlers) {
        logFilePath = newLogFilePath;
        logFileName = newLogFileName;
        loggerMap.put(className, this);

        // initialize the handlers
        if (!handlersInitialized || forceReloadHandlers) {
            FOKLogger.initLogHandlers();
        }

        log = Logger.getLogger(className);
        log.setLevel(Level.ALL);
    }

    /**
     * Resets all loggers. This makes no difference for the console logger, but the file logger will start logging into a new file.
     */
    public static void resetAllLoggers() {
        if (fileHandler != null) {
            fileHandler.close();
        }
        forceResetLogHandlersOnNextLogAction = true;
        loggerMap = new HashMap<>();
    }

    private static String combineLogPath() {
        String appDataPath = Common.getInstance().tryGetAppDataPath();
        if (appDataPath == null) {
            return null;
        } else {
            return appDataPath + "Logs";
        }
    }

    /**
     * @return the fileLogLevel
     */
    public static Level getFileLogLevel() {
        return fileLogLevel;
    }

    /**
     * @param newFileLogLevel the fileLogLevel to set
     */
    public static void setFileLogLevel(Level newFileLogLevel) {
        fileLogLevel = newFileLogLevel;

        // set the handlers Log Levels
        if (handlersInitialized) {
            fileHandler.setLevel(fileLogLevel);
        }
    }

    /**
     * @return the consoleLogLevel
     */

    public static Level getConsoleLogLevel() {
        return consoleLogLevel;
    }

    /**
     * @param newConsoleLogLevel the consoleLogLevel to set
     */

    public static void setConsoleLogLevel(Level newConsoleLogLevel) {
        consoleLogLevel = newConsoleLogLevel;

        // set the handlers Log Levels
        if (handlersInitialized) {
            consoleHandler.setLevel(consoleLogLevel);
        }
    }

    /**
     * @return the logFileName
     */

    public static String getLogFileName() {
        return logFileName.replace("DateTime", Common.getInstance().getLaunchTimeStamp());
    }

    /**
     * @return the logFilePath
     */

    public static String getLogFilePath() {
        return logFilePath;
    }

    public static String getLogFilePathAndName() {
        return logFilePath + File.separator + logFileName.replace("DateTime", Common.getInstance().getLaunchTimeStamp());
    }

    public static void initLogHandlers() {

        handlersInitialized = true;

        // Create a directory; all non-existent ancestor directories are
        // automatically created
        // Source:
        // http://stackoverflow.com/questions/4801971/how-to-create-empty-folder-in-java
        if (logFilePath != null) {
            try {
                Files.createDirectories(new File(logFilePath).toPath());
            } catch (IOException e) {
                fileLoggingDisabled = true;
                logFilePath = null;
            }
        }

        try {
            if (logFilePath != null) {
                fileHandler = new FileHandler(getLogFilePathAndName()) {
                    @Override
                    public synchronized void publish(LogRecord record) {
                        super.publish(record);
                        flush();
                    }
                };
            }
            consoleHandler = new Handler() {
                @Override
                public void publish(LogRecord record) {
                    if (record.getLevel().intValue() >= this.getLevel().intValue()) {
                        if (getFormatter() == null) {
                            setFormatter(new OneLineFormatter());
                        }

                        try {
                            String message = getFormatter().format(record);
                            if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                                System.err.write(message.getBytes());
                            } else {
                                System.out.write(message.getBytes());
                            }
                        } catch (Exception exception) {
                            reportError(null, exception, ErrorManager.FORMAT_FAILURE);
                        }
                    }

                }

                @Override
                public void flush() {
                }

                @Override
                public void close() throws SecurityException {
                }
            };
        } catch (IOException e) {
            // Not com.github.vatbub.common.core.logging into the logger as it is not yet initialized.
            e.printStackTrace();
        }

        // set the handlers Log Levels
        if (logFilePath != null) {
            fileHandler.setLevel(fileLogLevel);
        }
        consoleHandler.setLevel(consoleLogLevel);

        // Remove all existing parent handlers
        Logger globalLogger = Logger.getLogger("");
        Handler[] handlers = globalLogger.getHandlers();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < handlers.length; i++) {
            globalLogger.removeHandler(handlers[i]);
        }

        if (logFilePath != null) {
            globalLogger.addHandler(fileHandler);
        }
        globalLogger.addHandler(consoleHandler);

        if (logFilePath != null) {
            FOKLogger.info(FOKLogger.class.getName(), "Saving log file \n" + getLogFilePathAndName());
        } else {
            FOKLogger.info(FOKLogger.class.getName(), "Not saving the log in a file as no app name was specified");
        }
    }

    public static FOKLogger getLoggerByClassName(String className) {
        boolean forceRefreshLogger = Common.getInstance().getAppName() != null && logFilePath == null && !fileLoggingDisabled;
        if (forceResetLogHandlersOnNextLogAction) {
            forceRefreshLogger = true;
            handlersInitialized = false;
            forceResetLogHandlersOnNextLogAction = false;
        }
        if (!loggerMap.containsKey(className) || forceRefreshLogger) {
            // create a new logger
            loggerMap.remove(className);

            //noinspection deprecation
            return new FOKLogger(className);
        } else {
            // logger exists already
            return loggerMap.get(className);
        }
    }


    public static void log(String className, LogRecord record) {
        getLoggerByClassName(className).getLogger().log(record);
    }

    public static void log(String className, Level level, String msg) {
        getLoggerByClassName(className).getLogger().log(level, msg);
    }

    public static void log(String className, Level level, Supplier<String> msgSupplier) {
        getLoggerByClassName(className).getLogger().log(level, msgSupplier);
    }


    public static void log(String className, Level level, String msg, Object param1) {
        getLoggerByClassName(className).getLogger().log(level, msg, param1);
    }


    public static void log(String className, Level level, String msg, Object[] params) {
        getLoggerByClassName(className).getLogger().log(level, msg, params);
    }


    public static void log(String className, Level level, String msg, Throwable thrown) {
        getLoggerByClassName(className).getLogger().log(level, msg, thrown);
    }


    public static void log(String className, Level level, Throwable thrown, Supplier<String> msgSupplier) {
        getLoggerByClassName(className).getLogger().log(level, thrown, msgSupplier);
    }


    public static void logp(String className, Level level, String sourceClass, String sourceMethod, String msg) {
        getLoggerByClassName(className).getLogger().logp(level, sourceClass, sourceMethod, msg);
    }


    public static void logp(String className, Level level, String sourceClass, String sourceMethod, Supplier<String> msgSupplier) {
        getLoggerByClassName(className).getLogger().logp(level, sourceClass, sourceMethod, msgSupplier);
    }


    public static void logp(String className, Level level, String sourceClass, String sourceMethod,
                            String msg, Object param1) {
        getLoggerByClassName(className).getLogger().logp(level, sourceClass, sourceMethod, msg, param1);
    }


    public static void logp(String className, Level level, String sourceClass, String sourceMethod,
                            String msg, Object[] params) {
        getLoggerByClassName(className).getLogger().logp(level, sourceClass, sourceMethod, msg, params);
    }


    public static void logp(String className, Level level, String sourceClass, String sourceMethod,
                            String msg, Throwable thrown) {
        getLoggerByClassName(className).getLogger().logp(level, sourceClass, sourceMethod, msg, thrown);
    }


    public static void logp(String className, Level level, String sourceClass, String sourceMethod,
                            Throwable thrown, Supplier<String> msgSupplier) {
        getLoggerByClassName(className).getLogger().logp(level, sourceClass, sourceMethod, thrown, msgSupplier);
    }


    public static void logrb(String className, Level level, String sourceClass, String sourceMethod,
                             ResourceBundle bundle, String msg, Object... params) {
        getLoggerByClassName(className).getLogger().logrb(level, sourceClass, sourceMethod, bundle, msg, params);
    }


    public static void logrb(String className, Level level, String sourceClass, String sourceMethod,
                             ResourceBundle bundle, String msg, Throwable thrown) {
        getLoggerByClassName(className).getLogger().logrb(level, sourceClass, sourceMethod, bundle, msg, thrown);
    }


    public static void entering(String className, String sourceClass, String sourceMethod) {
        getLoggerByClassName(className).getLogger().entering(sourceClass, sourceMethod);
    }


    public static void entering(String className, String sourceClass, String sourceMethod, Object param1) {
        getLoggerByClassName(className).getLogger().entering(sourceClass, sourceMethod, param1);
    }


    public static void entering(String className, String sourceClass, String sourceMethod, Object[] params) {
        getLoggerByClassName(className).getLogger().entering(sourceClass, sourceMethod, params);
    }


    public static void exiting(String className, String sourceClass, String sourceMethod) {
        getLoggerByClassName(className).getLogger().exiting(sourceClass, sourceMethod);
    }


    public static void exiting(String className, String sourceClass, String sourceMethod, Object result) {
        getLoggerByClassName(className).getLogger().exiting(sourceClass, sourceMethod, result);
    }


    public static void throwing(String className, String sourceClass, String sourceMethod, Throwable thrown) {
        getLoggerByClassName(className).getLogger().throwing(sourceClass, sourceMethod, thrown);
    }


    public static void severe(String className, String msg) {
        log(className, Level.SEVERE, msg);
    }


    public static void warning(String className, String msg) {
        log(className, Level.WARNING, msg);
    }


    public static void info(String className, String msg) {
        log(className, Level.INFO, msg);
    }


    public static void config(String className, String msg) {
        log(className, Level.CONFIG, msg);
    }


    public static void fine(String className, String msg) {
        log(className, Level.FINE, msg);
    }


    public static void finer(String className, String msg) {
        log(className, Level.FINER, msg);
    }


    public static void finest(String className, String msg) {
        log(className, Level.FINEST, msg);
    }


    public static void severe(String className, Supplier<String> msgSupplier) {
        log(className, Level.SEVERE, msgSupplier);
    }


    public static void warning(String className, Supplier<String> msgSupplier) {
        log(className, Level.WARNING, msgSupplier);
    }


    public static void info(String className, Supplier<String> msgSupplier) {
        log(className, Level.INFO, msgSupplier);
    }


    public static void config(String className, Supplier<String> msgSupplier) {
        log(className, Level.CONFIG, msgSupplier);
    }


    public static void fine(String className, Supplier<String> msgSupplier) {
        log(className, Level.FINE, msgSupplier);
    }


    public static void finer(String className, Supplier<String> msgSupplier) {
        log(className, Level.FINER, msgSupplier);
    }


    public static void finest(String className, Supplier<String> msgSupplier) {
        log(className, Level.FINEST, msgSupplier);
    }

    /**
     * Once called, all uncaught exceptions will be written to the log too
     */
    public static void enableLoggingOfUncaughtExceptions() {
        Thread.setDefaultUncaughtExceptionHandler(logUncaughtException);
    }

    /**
     * Once called, no uncaught exception will be written to the log anymore
     */
    public static void disableLoggingOfUncaughtExceptions() {
        Thread.setDefaultUncaughtExceptionHandler(null);
    }

    /**
     * Returns the actual Logger object by {@link Logger}
     *
     * @return The actual Logger object by {@link Logger}
     */
    public Logger getLogger() {
        return log;
    }
}
