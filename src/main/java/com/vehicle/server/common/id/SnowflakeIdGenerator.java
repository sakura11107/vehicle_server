package com.vehicle.server.common.id;

import org.springframework.stereotype.Component;

/**
 * 简易雪花算法 ID 生成器。
 */
@Component
public class SnowflakeIdGenerator {

    private static final long EPOCH = 1704067200000L;
    private static final long SEQUENCE_MASK = 4095L;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            while (sequence == 0) {
                timestamp = System.currentTimeMillis();
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << 12) | sequence;
    }
}
