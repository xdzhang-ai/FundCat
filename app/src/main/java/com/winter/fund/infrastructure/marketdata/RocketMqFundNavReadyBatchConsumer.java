package com.winter.fund.infrastructure.marketdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.fund.common.config.RocketMqProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.SessionCredentialsProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.consumer.PushConsumerBuilder;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "fundcat.market-data.rocketmq", name = "enabled", havingValue = "true")
public class RocketMqFundNavReadyBatchConsumer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(RocketMqFundNavReadyBatchConsumer.class);

    private final ObjectMapper objectMapper;
    private final FundNavConfirmedConsumer fundNavConfirmedConsumer;
    private final RocketMqProperties rocketMqProperties;

    private volatile boolean running;
    private PushConsumer pushConsumer;

    public RocketMqFundNavReadyBatchConsumer(
        ObjectMapper objectMapper,
        FundNavConfirmedConsumer fundNavConfirmedConsumer,
        RocketMqProperties rocketMqProperties
    ) {
        this.objectMapper = objectMapper;
        this.fundNavConfirmedConsumer = fundNavConfirmedConsumer;
        this.rocketMqProperties = rocketMqProperties;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        if (!"v5".equalsIgnoreCase(rocketMqProperties.getConsumerMode())) {
            log.info("Skipping RocketMQ v5 consumer startup because consumerMode={}", rocketMqProperties.getConsumerMode());
            running = true;
            return;
        }
        try {
            ClientServiceProvider provider = ClientServiceProvider.loadService();
            PushConsumerBuilder builder = provider.newPushConsumerBuilder()
                .setClientConfiguration(buildClientConfiguration())
                .setConsumerGroup(rocketMqProperties.getConsumerGroup())
                .setSubscriptionExpressions(Map.of(
                    rocketMqProperties.getTopic(),
                    buildFilterExpression()
                ))
                .setMessageListener(this::consume)
                .setConsumptionThreadCount(20)
                .setMaxCacheMessageSizeInBytes(64 * 1024 * 1024)
                .setMaxCacheMessageCount(1024);
            pushConsumer = builder.build();
            running = true;
            log.info(
                "RocketMQ push consumer started, endpoints={}, topic={}, tag={}, consumerGroup={}, sslEnabled={}",
                rocketMqProperties.getEndpoints(),
                rocketMqProperties.getTopic(),
                rocketMqProperties.getTag(),
                rocketMqProperties.getConsumerGroup(),
                rocketMqProperties.isSslEnabled()
            );
        } catch (ClientException exception) {
            throw new IllegalStateException("Failed to start RocketMQ push consumer", exception);
        }
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        if (pushConsumer != null) {
            try {
                pushConsumer.close();
            } catch (IOException exception) {
                log.warn("Failed to close RocketMQ push consumer cleanly, consumerGroup={}", rocketMqProperties.getConsumerGroup(), exception);
            }
            pushConsumer = null;
        }
        running = false;
        log.info("RocketMQ push consumer stopped, consumerGroup={}", rocketMqProperties.getConsumerGroup());
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    private ConsumeResult consume(MessageView messageView) {
        try {
            String body = StandardCharsets.UTF_8.decode(messageView.getBody()).toString();
            FundNavReadyBatchMessage message = objectMapper.readValue(body, FundNavReadyBatchMessage.class);
            fundNavConfirmedConsumer.onFundNavReadyBatch(message);
            log.info(
                "RocketMQ fund_nav_ready_batch consumed, messageId={}, navDate={}, fundCount={}",
                messageView.getMessageId(),
                message.navDate(),
                message.count()
            );
            return ConsumeResult.SUCCESS;
        } catch (Exception exception) {
            log.error(
                "Failed to consume RocketMQ fund_nav_ready_batch message, messageId={}",
                messageView.getMessageId(),
                exception
            );
            return ConsumeResult.FAILURE;
        }
    }

    private ClientConfiguration buildClientConfiguration() {
        ClientConfigurationBuilder builder = ClientConfiguration.newBuilder()
            .setEndpoints(rocketMqProperties.getEndpoints())
            .setRequestTimeout(Duration.ofSeconds(5))
            .enableSsl(rocketMqProperties.isSslEnabled());
        if (StringUtils.hasText(rocketMqProperties.getNamespace())) {
            builder.setNamespace(rocketMqProperties.getNamespace());
        }
        if (StringUtils.hasText(rocketMqProperties.getAccessKey()) && StringUtils.hasText(rocketMqProperties.getSecretKey())) {
            SessionCredentialsProvider credentialsProvider =
                new StaticSessionCredentialsProvider(rocketMqProperties.getAccessKey(), rocketMqProperties.getSecretKey());
            builder.setCredentialProvider(credentialsProvider);
        }
        return builder.build();
    }

    private FilterExpression buildFilterExpression() {
        if (!StringUtils.hasText(rocketMqProperties.getTag())) {
            return FilterExpression.SUB_ALL;
        }
        return new FilterExpression(rocketMqProperties.getTag());
    }
}
