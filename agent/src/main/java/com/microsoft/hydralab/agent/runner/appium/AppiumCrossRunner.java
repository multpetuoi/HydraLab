// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner.appium;

import com.microsoft.hydralab.common.entity.center.TestTaskSpec;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.TestTask;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Set;

@Service
public class AppiumCrossRunner extends AppiumRunner {
    @Value("${app.registry.name}")
    String agentName;

    @Override
    protected Set<DeviceInfo> chooseDevices(TestTaskSpec testTaskSpec) {
        Set<DeviceInfo> allActiveConnectedDevice = deviceManager.getActiveDeviceList(log);
        log.info("Choosing devices from {}", allActiveConnectedDevice.size());
        Assert.notNull(allActiveConnectedDevice, "No connected device!");
        Assert.isTrue(allActiveConnectedDevice.size() == 1, "No connected device!");

        return allActiveConnectedDevice;
    }

    @Override
    public DeviceTestTask initDeviceTestTask(DeviceInfo deviceInfo, TestTask testTask, Logger logger) {
        DeviceTestTask deviceTestTask = super.initDeviceTestTask(deviceInfo, testTask, logger);
        String deviceName = System.getProperties().getProperty("os.name") + "-" + agentName + "-" + deviceInfo.getName();
        deviceTestTask.setDeviceName(deviceName);
        return deviceTestTask;
    }
}