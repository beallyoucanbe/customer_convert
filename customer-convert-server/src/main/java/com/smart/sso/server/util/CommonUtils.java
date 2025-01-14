package com.smart.sso.server.util;

import com.smart.sso.server.model.dto.OriginChat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static String encodeParameters(String url) throws UnsupportedEncodingException {
        // 正则表达式匹配查询字符串中的参数对
        Pattern pattern = Pattern.compile("([^&=]+)=([^&]*)");
        Matcher matcher = pattern.matcher(url);
        StringBuffer encodedUrl = new StringBuffer();

        while (matcher.find()) {
            String paramName = matcher.group(1);
            String paramValue = matcher.group(2);
            // 对参数值进行编码，参数名保持不变
            matcher.appendReplacement(encodedUrl, paramName + "=" + URLEncoder.encode(paramValue, "UTF-8"));
        }
        matcher.appendTail(encodedUrl);

        return encodedUrl.toString();
    }

    public static List<OriginChat.Message> getMessageListFromOriginChat(String chatContent) {
        List<OriginChat.Message> result = new ArrayList<>();
        // 空字符串检查
        if (chatContent == null || chatContent.trim().isEmpty()) {
            return result;
        }
        // 正则表达式：匹配时间部分
        String regex = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(chatContent);

        if (matcher.find()) {
            String timeCurrent = matcher.group();
            int timeCurrentStartIndex = matcher.start();// 表示第一个时间数字开始的下标
            int timeCurrentEndIndex = matcher.end(); // 表示最后一个数字结束的下标 + 1
            String timeNext;
            int timeNextStartIndex;
            int timeNextEndIndex;
            int roleStartIndex = 0;
            while (matcher.find()) {
                // 还有下一个时间，循环获取
                timeNext = matcher.group();
                timeNextStartIndex = matcher.start();
                timeNextEndIndex = matcher.end();
                OriginChat.Message message = new OriginChat.Message();
                message.setRole(chatContent.substring(roleStartIndex, timeCurrentStartIndex).trim());
                message.setTime(timeCurrent);
                boolean isFindNoEmpty = false;
                for (int i = timeNextStartIndex - 1; i > timeCurrentEndIndex; i--) {
                    // 是否找到一个非空白字符
                    if (isFindNoEmpty && !StringUtils.hasText(String.valueOf(chatContent.charAt(i)))) {
                        roleStartIndex = i + 1;
                        message.setContent(chatContent.substring(timeCurrentEndIndex, roleStartIndex).trim());
                        result.add(message);
                        break;
                    } else if (StringUtils.hasText(String.valueOf(chatContent.charAt(i)))) {
                        isFindNoEmpty = true;
                    }
                }
                timeCurrent = timeNext;
                timeCurrentStartIndex = timeNextStartIndex;
                timeCurrentEndIndex = timeNextEndIndex;
            }
            OriginChat.Message message = new OriginChat.Message();
            message.setRole(chatContent.substring(roleStartIndex, timeCurrentStartIndex).trim());
            message.setTime(timeCurrent);
            message.setContent(chatContent.substring(timeCurrentEndIndex).trim());
            result.add(message);
        }
        return result;
    }

    public static OriginChat getOriginChatFromChatText(String callId, String chatContent) {
        if (StringUtils.isEmpty(chatContent)) {
            return null;
        }
        OriginChat originChat = new OriginChat();
        originChat.setContents(getMessageListFromOriginChat(chatContent));
        originChat.setId(callId);
        return originChat;
    }

    public static String convertStringFromList(List<String> stringList) {
        if (CollectionUtils.isEmpty(stringList)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String string : stringList) {
            sb.append(string).append(" \n ");
        }
        return sb.toString();
    }

    public static int  calculateDaysDifference(LocalDateTime startDateTime) {
        // 解析输入的时间字符串
        LocalDateTime now = LocalDateTime.now();

        // 如果输入时间晚于当前时间，抛出异常
        if (startDateTime.isAfter(now)) {
            throw new IllegalArgumentException("Input time must be earlier than the current time.");
        }

        // 转换为LocalDate，忽略时间部分
        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate currentDate = now.toLocalDate();
        int workdays = 0;
        // 从起始日期开始逐天迭代，计算工作日
        while (!startDate.isAfter(currentDate)) {
            if (isWorkday(startDate)) {
                workdays++;
            }
            startDate = startDate.plusDays(1);
        }
        return workdays;
    }

    // 判断是否为工作日（周一到周五）
    private static boolean isWorkday(LocalDate date) {
        switch (date.getDayOfWeek()) {
            case MONDAY:
            case TUESDAY:
            case WEDNESDAY:
            case THURSDAY:
            case FRIDAY:
                return true;
            default:
                return false;
        }
    }

    public static String getTimeStringWithChina(int minutes){
        // 计算小时数和剩余的分钟数
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        // 如果小时数大于0，则返回小时和分钟的格式
        if (hours > 0) {
            return hours + "小时" + remainingMinutes + "分钟";
        } else {
            // 如果小时数为0，则只返回分钟数
            return remainingMinutes + "分钟";
        }
    }

    public static String getTimeStringWithMinute(int seconds){
        // 计算分钟数和剩余的秒数
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;

        // 如果小时数大于0，则返回小时和分钟的格式
        if (minutes > 0) {
            return minutes + "min" + remainingSeconds + "s";
        } else {
            // 如果小时数为0，则只返回分钟数
            return remainingSeconds + "s";
        }
    }
}
