package com.ctrip.framework.apollo.portal.listener;

import com.ctrip.framework.apollo.common.constants.ReleaseOperation;
import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.component.emailbuilder.GrayPublishEmailBuilder;
import com.ctrip.framework.apollo.portal.component.emailbuilder.MergeEmailBuilder;
import com.ctrip.framework.apollo.portal.component.emailbuilder.NormalPublishEmailBuilder;
import com.ctrip.framework.apollo.portal.component.emailbuilder.RollbackEmailBuilder;
import com.ctrip.framework.apollo.portal.entity.bo.Email;
import com.ctrip.framework.apollo.portal.entity.bo.ReleaseHistoryBO;
import com.ctrip.framework.apollo.portal.service.ReleaseHistoryService;
import com.ctrip.framework.apollo.portal.spi.EmailService;
import com.ctrip.framework.apollo.portal.spi.MQService;
import com.ctrip.framework.apollo.tracer.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 配置发布 Listener
 */
@Component
public class ConfigPublishListener {

    @Autowired
    private ReleaseHistoryService releaseHistoryService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private NormalPublishEmailBuilder normalPublishEmailBuilder;
    @Autowired
    private GrayPublishEmailBuilder grayPublishEmailBuilder;
    @Autowired
    private RollbackEmailBuilder rollbackEmailBuilder;
    @Autowired
    private MergeEmailBuilder mergeEmailBuilder;
    @Autowired
    private PortalConfig portalConfig;
    @Autowired
    private MQService mqService;

    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        executorService = Executors.newSingleThreadExecutor(ApolloThreadFactory.create("ConfigPublishNotify", true));
    }

    @EventListener
    public void onConfigPublish(ConfigPublishEvent event) {
        executorService.submit(new ConfigPublishNotifyTask(event.getConfigPublishInfo()));
    }


    private class ConfigPublishNotifyTask implements Runnable {

        private ConfigPublishEvent.ConfigPublishInfo publishInfo;

        ConfigPublishNotifyTask(ConfigPublishEvent.ConfigPublishInfo publishInfo) {
            this.publishInfo = publishInfo;
        }

        @Override
        public void run() {
            ReleaseHistoryBO releaseHistory = getReleaseHistory();
            // 获得不到 ReleaseHistoryBO 对象，返回
            if (releaseHistory == null) {
                Tracer.logError("Load release history failed", null);
                return;
            }
            // 发送邮件
            sendPublishEmail(releaseHistory);
            // 发送 MQ 消息
            sendPublishMsg(releaseHistory);
        }

        /**
         * 调用 Admin Service ，获得对应的 ReleaseHistoryBO 对象
         *
         * @return ReleaseHistoryBO 对象
         */
        private ReleaseHistoryBO getReleaseHistory() {
            Env env = publishInfo.getEnv();
            // 获得发布操作类型
            int operation = publishInfo.isMergeEvent() ? ReleaseOperation.GRAY_RELEASE_MERGE_TO_MASTER :
                    publishInfo.isRollbackEvent() ? ReleaseOperation.ROLLBACK :
                            publishInfo.isNormalPublishEvent() ? ReleaseOperation.NORMAL_RELEASE :
                                    publishInfo.isGrayPublishEvent() ? ReleaseOperation.GRAY_RELEASE : -1;
            // 操作类型未知，直接返回
            if (operation == -1) {
                return null;
            }
            // 若是回滚操作，获得前一个版本
            if (publishInfo.isRollbackEvent()) {
                return releaseHistoryService.findLatestByPreviousReleaseIdAndOperation(env, publishInfo.getPreviousReleaseId(), operation);
            // 若是非回滚操作，获得最新版本
            } else {
                return releaseHistoryService.findLatestByReleaseIdAndOperation(env, publishInfo.getReleaseId(), operation);
            }
        }

        private void sendPublishEmail(ReleaseHistoryBO releaseHistory) {
            Env env = publishInfo.getEnv();

            // 判断当前 Env 是否支持发邮件
            if (!portalConfig.emailSupportedEnvs().contains(env)) {
                return;
            }

            int realOperation = releaseHistory.getOperation();

            // 创建 Email 对象
            Email email = null;
            try {
                email = buildEmail(env, releaseHistory, realOperation);
            } catch (Throwable e) {
                Tracer.logError("build email failed.", e);
            }

            // 发送邮件
            if (email != null) {
                emailService.send(email);
            }
        }

        private void sendPublishMsg(ReleaseHistoryBO releaseHistory) {
            mqService.sendPublishMsg(publishInfo.getEnv(), releaseHistory);
        }

        /**
         * 创建 Email 对象
         *
         * @param env Env
         * @param releaseHistory ReleaseHistoryBO 对象
         * @param operation 发布操作类型
         * @return Email 对象
         */
        private Email buildEmail(Env env, ReleaseHistoryBO releaseHistory, int operation) {
            switch (operation) {
                case ReleaseOperation.GRAY_RELEASE: {
                    return grayPublishEmailBuilder.build(env, releaseHistory);
                }
                case ReleaseOperation.NORMAL_RELEASE: {
                    return normalPublishEmailBuilder.build(env, releaseHistory);
                }
                case ReleaseOperation.ROLLBACK: {
                    return rollbackEmailBuilder.build(env, releaseHistory);
                }
                case ReleaseOperation.GRAY_RELEASE_MERGE_TO_MASTER: {
                    return mergeEmailBuilder.build(env, releaseHistory);
                }
                default:
                    return null;
            }
        }
    }

}
