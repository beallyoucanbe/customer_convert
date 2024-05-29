package com.smart.sso.server.util;

import java.util.Random;

public class CommonUtils {

    public static String generatePrimaryKey() {
        // 获取当前时间戳（毫秒级别）
        long currentTimeMillis = System.currentTimeMillis();

        // 获取时间戳的后10位
        long timeComponent = currentTimeMillis % 10000000000L;

        // 生成一个5位的随机数
        Random random = new Random();
        int randomComponent = 10000 + random.nextInt(90000);

        // 组合时间戳和随机数，形成15位主键
        String primaryKey = String.format("%010d%05d", timeComponent, randomComponent);

        return primaryKey;
    }
}
