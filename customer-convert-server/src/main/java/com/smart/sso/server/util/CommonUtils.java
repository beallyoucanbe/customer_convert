package com.smart.sso.server.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

@Slf4j
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

    /**
     * 删除文本中的标点符号
     *
     * @param text
     * @return
     */
    public static String deletePunctuation(Object text) {
        if (StringUtils.isEmpty(text)) {
            return (String) text;
        }
        // 定义一个正则表达式来匹配所有中英文标点符号
        String regex = "[\\p{P}\\p{S}]";
        // 使用replaceAll方法将匹配到的标点符号替换为空字符串
        return text.toString().replaceAll(regex, "");
    }

    public static void appendTextToFile(String filePath, String text) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(text);
            writer.newLine();
        } catch (IOException e) {
            log.error("保存数据到文件失败");
        }
    }
}
