package org.artifactory.storage.binary;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.mail.MailService;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.schedule.*;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.binstore.service.BinaryStore;
import org.artifactory.version.CompoundVersionDetails;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author gidis
 */
@Service
@Reloadable(beanClass = BinaryStoreService.class,
        initAfter = {TaskService.class})
public class BinaryStoreServiceImpl implements BinaryStoreService {
    private static final Logger log = LoggerFactory.getLogger(BinaryStoreService.class);

    @Autowired
    private TaskService taskService;

    @Override
    public void init() {
        registersErrorPollingJob();
    }

    /**
     * creates & starts HeartbeatJob
     */
    private void registersErrorPollingJob() {
        TaskBase errorNotificationJob = TaskUtils.createRepeatingTask(BinaryStoreErrorNotificationJob.class,
                TimeUnit.SECONDS.toMillis(ConstantValues.binaryStoreErrorNotificationsIntervalSecs.getLong()),
                TimeUnit.SECONDS.toMillis(ConstantValues.binaryStoreErrorNotificationsStaleIntervalSecs.getLong()));
        taskService.startTask(errorNotificationJob, false);
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {

    }

    @JobCommand(singleton = true, runOnlyOnPrimary = false, description = "binary store error notification",
            schedulerUser = TaskUser.SYSTEM, manualUser = TaskUser.SYSTEM)
    public static class BinaryStoreErrorNotificationJob extends QuartzCommand {
        @Override
        protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
            BinaryStore binaryStore = ContextHelper.get().beanForType(BinaryStore.class);
            List<String> errorMessages = binaryStore.getAndManageErrors();
            for (String error : errorMessages) {
                AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
                MailService mailService = ContextHelper.get().beanForType(MailService.class);
                CoreAddons coreAddons = addonsManager.addonByType(CoreAddons.class);
                List<String> adminEmails = coreAddons.getUsersForBackupNotifications();
                for (String adminEmail : adminEmails) {
                    mailService.sendMail(new String[]{adminEmail}, "Critical binary provider error", "Critical Error occurred in Artifactory binary provider - " + error + ", disabling single binary provider");
                }
            }
        }
    }
}

