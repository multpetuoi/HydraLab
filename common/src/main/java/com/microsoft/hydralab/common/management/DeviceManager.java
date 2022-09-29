// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.management;

import com.android.ddmlib.InstallException;
import com.android.ddmlib.TimeoutException;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.util.IOSUtils;
import com.microsoft.hydralab.common.util.LogUtils;
import com.microsoft.hydralab.common.util.ThreadPoolUtil;
import com.microsoft.hydralab.common.util.blob.BlobStorageClient;
import io.appium.java_client.appmanagement.ApplicationState;
import io.appium.java_client.ios.IOSDriver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.android.ddmlib.IDevice.DeviceState;


public abstract class DeviceManager {
    public static final String LOGGER_PREFIX = "logger.devices.";
    static final Logger classLogger = LoggerFactory.getLogger(DeviceManager.class);
    private final Map<String, TestTask> runningTestTask = new HashMap<>();
    protected BlobStorageClient blobStorageClient;
    protected DeviceStabilityMonitor deviceStabilityMonitor;
    protected File testBaseDir;
    protected String testBaseDirUrlMapping;
    protected File deviceLogBaseDir;
    protected File screenshotDir;
    protected String deviceFolderUrlPrefix;
    protected String deviceStoragePath;

    protected AppiumServerManager appiumServerManager;

    public static MobileDeviceState mobileDeviceStateMapping(DeviceState adbState) {
        if (adbState == null) {
            return MobileDeviceState.OTHER;
        }

        switch (adbState) {
            case ONLINE:
                return MobileDeviceState.ONLINE;
            case OFFLINE:
                return MobileDeviceState.OFFLINE;
            case DISCONNECTED:
                return MobileDeviceState.DISCONNECTED;
            default:
                return MobileDeviceState.OTHER;
        }
    }

    public AppiumServerManager getAppiumServerManager() {
        return appiumServerManager;
    }

    public void setAppiumServerManager(AppiumServerManager appiumServerManager) {
        this.appiumServerManager = appiumServerManager;
    }

    public Map<String, TestTask> getRunningTestTask() {
        return runningTestTask;
    }

    public BlobStorageClient getBlobStorageClient() {
        return blobStorageClient;
    }

    public void setBlobStorageClient(BlobStorageClient blobStorageClient) {
        this.blobStorageClient = blobStorageClient;
    }

    public DeviceStabilityMonitor getDeviceStabilityMonitor() {
        return this.deviceStabilityMonitor;
    }

    public void setDeviceStabilityMonitor(DeviceStabilityMonitor deviceStabilityMonitor) {
        this.deviceStabilityMonitor = deviceStabilityMonitor;
    }

    public File getTestBaseDir() {
        return testBaseDir;
    }

    public void setTestBaseDir(File testBaseDir) {
        this.testBaseDir = testBaseDir;
    }

    public String getTestBaseDirUrlMapping() {
        return testBaseDirUrlMapping;
    }

    public void setTestBaseDirUrlMapping(String testBaseDirUrlMapping) {
        this.testBaseDirUrlMapping = testBaseDirUrlMapping;
    }

    public File getDeviceLogBaseDir() {
        return deviceLogBaseDir;
    }

    public void setDeviceLogBaseDir(File deviceLogBaseDir) {
        this.deviceLogBaseDir = deviceLogBaseDir;
    }

    public File getScreenshotDir() {
        return screenshotDir;
    }

    public void setScreenshotDir(File screenshotDir) {
        this.screenshotDir = screenshotDir;
    }

    public String getDeviceFolderUrlPrefix() {
        return deviceFolderUrlPrefix;
    }

    public void setDeviceFolderUrlPrefix(String deviceFolderUrlPrefix) {
        this.deviceFolderUrlPrefix = deviceFolderUrlPrefix;
    }

    public String getDeviceStoragePath() {
        return deviceStoragePath;
    }

    public void setDeviceStoragePath(String deviceStoragePath) {
        this.deviceStoragePath = deviceStoragePath;
    }

    public abstract void init() throws Exception;

    public abstract Set<DeviceInfo> getDeviceList(@Nullable Logger logger);

    public abstract Set<DeviceInfo> getActiveDeviceList(@Nullable Logger logger);

    public abstract File getScreenShot(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) throws Exception;

    public abstract void resetDeviceByTestId(@NotNull String testId, @Nullable Logger logger);

    public abstract void wakeUpDevice(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger);

    public abstract void backToHome(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger);

    public abstract void grantPermission(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @NotNull String permissionName, @Nullable Logger logger);

    public abstract void addToBatteryWhiteList(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @NotNull Logger logger);

    public abstract boolean installApp(@NotNull DeviceInfo deviceInfo, @NotNull String packagePath, @Nullable Logger logger) throws InstallException;

    public abstract boolean uninstallApp(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger) throws InstallException;

    public abstract void resetPackage(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger);

    public abstract ScreenRecorder getScreenRecorder(@NotNull DeviceInfo deviceInfo, @NotNull File folder, @Nullable Logger logger);

    public boolean grantAllTaskNeededPermissions(@NotNull DeviceInfo deviceInfo, @NotNull TestTask task, @Nullable Logger logger) {
        return false;
    }

    public boolean grantAllPackageNeededPermissions(@NotNull DeviceInfo deviceInfo, @NotNull File packageFile, @NotNull String targetPackage, boolean allowCustomizedPermissions, @Nullable Logger logger) {
        return false;
    }

    public void safeSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            classLogger.error(e.getMessage(), e);
        }
    }

    public Logger getDeviceLogger(DeviceInfo device) {
        String file = deviceLogBaseDir.getAbsolutePath() + "/" + device.getName() + "/device_control.log";
        return LogUtils.getLoggerWithRollingFileAppender(LOGGER_PREFIX + device.getSerialNum(), file, "%d %logger{0} %p [%t] - %m%n");
    }

    public String getTestBaseRelPathInUrl(File report) {
        return report.getAbsolutePath().replace(testBaseDir.getAbsolutePath(), testBaseDirUrlMapping).replace(File.separator, "/");
    }

    public void updateScreenshotImageAsyncDelay(@NotNull DeviceInfo deviceInfo, long delayMillis, @NotNull FileAvailableCallback fileAvailableCallback, @NotNull Logger logger) {
        ThreadPoolUtil.executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delayMillis);
                    File imageFile = getScreenShot(deviceInfo, logger);
                    if (fileAvailableCallback != null) {
                        fileAvailableCallback.onFileReady(imageFile);
                    }
                } catch (TimeoutException te) {
                    classLogger.error("{}: {}, updateScreenshotImageAsyncDelay", te.getClass().getSimpleName(), te.getMessage());
                } catch (Exception e) {
                    classLogger.error(e.getMessage(), e);
                }
            }
        });
    }

    public void updateAllDeviceInfo() {
    }

    public abstract LogCollector getLogCollector(@NotNull DeviceInfo deviceInfo, @NotNull String pkgName, @NotNull DeviceTestTask deviceTestResult, @NotNull Logger logger);

    public abstract void setProperty(@NotNull DeviceInfo deviceInfo, @NotNull String property, String val, @Nullable Logger logger);

    public abstract void updateIsPrivateByDeviceSerial(@NotNull String deviceSerial, @NotNull Boolean isPrivate);

    public abstract boolean setLauncherAsDefault(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @NotNull String defaultActivity, @Nullable Logger logger);

    public abstract boolean isAppInstalled(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger);

    public abstract boolean grantProjectionAndBatteryPermission(@NotNull DeviceInfo deviceInfo, @NotNull String recordPackageName, @Nullable Logger logger);

    public abstract void testDeviceSetup(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) throws IOException;

    public abstract void removeFileInDevice(DeviceInfo deviceInfo, String pathOnDevice, Logger logger);

    public abstract void testDeviceUnset(DeviceInfo deviceInfo, Logger logger);

    abstract public WebDriver getMobileAppiumDriver(DeviceInfo deviceInfo, Logger logger);

    abstract public void quitMobileAppiumDriver(DeviceInfo deviceInfo, Logger logger);

    protected boolean isAppRunningForeground(DeviceInfo deviceInfo, String packageName, Logger logger) {
        IOSDriver iOSDriver = appiumServerManager.getIOSDriver(deviceInfo, logger);
        ApplicationState state = iOSDriver.queryAppState(packageName);
        boolean result = (state == ApplicationState.RUNNING_IN_FOREGROUND);
        if (!result) {
            logger.info("State of App " + packageName + " is: " + state.toString());
        }
        return result;
    }

    public void runAppiumMonkey(DeviceInfo deviceInfo, String packageName, int round, Logger logger) {
        try {
            for (int i = 0; i < round; i++) {
                logger.info("Monkey Test Round " + i + "[Started]");
                WebDriver driver = getMobileAppiumDriver(deviceInfo, logger);
                // Select all the leaf nodes
                List<WebElement> eleList = driver.findElements(By.xpath("//*[not(*)]"));
                int count = eleList.size();
                logger.info("Found " + count + " element(s)");
                if (StringUtils.isEmpty(packageName)) {
                    if (count == 0) {
                        logger.info("No element Found, Back to Home Screen.");
                        backToHome(deviceInfo, logger);
                        eleList = driver.findElements(By.xpath("//*"));
                        count = eleList.size();
                    }
                } else {
                    if (count == 0 || !isAppRunningForeground(deviceInfo, packageName, logger)) {
                        logger.info("No element Found or App is Running in Background, Back to Home Screen and Restart App.");
                        backToHome(deviceInfo, logger);
                        IOSUtils.launchApp(deviceInfo.getSerialNum(), packageName, logger);
                        eleList = driver.findElements(By.xpath("//*"));
                        count = eleList.size();
                    }
                }
                if (count <= 0) continue;
                int r = new Random().nextInt(count);
                WebElement e = eleList.get(r);
                try {
                    String name = e.getText();
                    logger.info("Select NO. " + r + " element: " + name);
                    e.click();
                } catch (StaleElementReferenceException | ElementNotInteractableException ignore) {
                    // Cached element is not exist any more skip this try.
                }
                logger.info("Monkey Test Round " + i + "[Done]");
            }
        } catch (WebDriverException e) {
            e.printStackTrace();
            logger.info("Monkey Test Exit with Error, Quit the Driver. ");
            quitMobileAppiumDriver(deviceInfo, logger);
        }
    }

    public boolean runAppiumT2CTest(DeviceInfo deviceInfo, File jsonFile, Logger reportLogger) {
        return true;
    }

    public enum MobileDeviceState {
        OFFLINE,
        ONLINE,
        DISCONNECTED,
        TESTING,
        UNSTABLE,
        OTHER
    }

    public interface FileAvailableCallback {
        void onFileReady(File file);
    }

    public interface DeviceStatusMonitor {
        void onDeviceInactive(DeviceInfo deviceInfo);

        void onDeviceConnected(DeviceInfo deviceInfo);
    }
}