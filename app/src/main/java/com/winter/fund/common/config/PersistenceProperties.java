package com.winter.fund.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fundcat.persistence")
public class PersistenceProperties {

    private int batchSize = 100;

    /**
     * 获取批量写入条数。
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * 设置批量写入条数。
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}

