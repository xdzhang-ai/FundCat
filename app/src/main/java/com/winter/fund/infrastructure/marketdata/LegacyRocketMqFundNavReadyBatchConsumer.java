package com.winter.fund.infrastructure.marketdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.fund.common.config.RocketMqProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "fundcat.market-data.rocketmq", name = "enabled", havingValue = "true")
public class LegacyRocketMqFundNavReadyBatchConsumer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(LegacyRocketMqFundNavReadyBatchConsumer.class);

    private final ObjectMapper objectMapper;
    private final FundNavConfirmedConsumer fundNavConfirmedConsumer;
    private final RocketMqProperties rocketMqProperties;

    private volatile boolean running;
    private DefaultMQPushConsumer consumer;

    public LegacyRocketMqFundNavReadyBatchConsumer(
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
        if (!"legacy".equalsIgnoreCase(rocketMqProperties.getConsumerMode())) {
            log.info("Skipping RocketMQ legacy consumer startup because consumerMode={}", rocketMqProperties.getConsumerMode());
            running = true;
            return;
        }
        try {
            consumer = new DefaultMQPushConsumer(rocketMqProperties.getConsumerGroup());
            consumer.setNamesrvAddr(rocketMqProperties.getEndpoints());
            consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
            consumer.subscribe(rocketMqProperties.getTopic(), buildTagExpression());
            consumer.registerMessageListener(new LegacyMessageListener());
            consumer.start();
            running = true;
            log.info(
                "RocketMQ legacy consumer started, namesrvAddr={}, topic={}, tag={}, consumerGroup={}",
                rocketMqProperties.getEndpoints(),
                rocketMqProperties.getTopic(),
                rocketMqProperties.getTag(),
                rocketMqProperties.getConsumerGroup()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to start RocketMQ legacy consumer", exception);
        }
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        if (consumer != null) {
            consumer.shutdown();
            consumer = null;
        }
        running = false;
        log.info("RocketMQ legacy consumer stopped, consumerGroup={}", rocketMqProperties.getConsumerGroup());
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
        return Integer.MAX_VALUE - 1;
    }

    private String buildTagExpression() {
        if (!StringUtils.hasText(rocketMqProperties.getTag())) {
            return "*";
        }
        return rocketMqProperties.getTag();
    }

    private class LegacyMessageListener implements MessageListenerConcurrently {
        @Override
        public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> messages, ConsumeConcurrentlyContext context) {
            for (MessageExt messageExt : messages) {
                try {
                    String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
                    FundNavReadyBatchMessage message = objectMapper.readValue(body, FundNavReadyBatchMessage.class);
                    fundNavConfirmedConsumer.onRocketMqFundNavReadyBatch(messageExt.getMsgId(), message);
                    log.info(
                        "RocketMQ legacy fund_nav_ready_batch accepted for async processing, messageId={}, navDate={}, fundCount={}",
                        messageExt.getMsgId(),
                        message.navDate(),
                        message.count()
                    );
                } catch (Exception exception) {
                    log.error(
                        "Failed to deserialize or dispatch RocketMQ legacy fund_nav_ready_batch message, messageId={}; message will be acknowledged without broker retry",
                        messageExt.getMsgId(),
                        exception
                    );
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }
    }
}
