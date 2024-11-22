package com.smart.sso.server.constant;

import java.util.HashSet;
import java.util.Set;

/**
 * 应用常量
 */
public class AppConstant {

    // 存cookie中TGT名称，和Cas保存一致
    public static final String TGC = "cs_token";

    // 登录页
    public static final String LOGIN_PATH = "/login";

    public static final String REFRESH_CONVERSION_RATE = "REFRESH_CONVERSION_RATE";
    public static final String SEND_MESSAGE_STATE = "SEND_MESSAGE_STATE";

    public static final String PURCHASE_STATE_CHECK = "PURCHASE_STATE_CHECK";

    public static final String REFRESH_CUSTOMER_COMPLETE_DESCRIBE = "REFRESH_CUSTOMER_COMPLETE_DESCRIBE";

    public static final String SOURCEID_KEY_PREFIX = "processed:";

    public static final String CUSTOMER_DASHBOARD_URL = "http://172.16.192.61:8086/preview/33/dashboard/27";

    public static final String GET_SECRET_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s";

    public static final String SEND_APPLICATION_MESSAGE_URL = " https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s";

    public static final String CUSTOMER_SUMMARY_MARKDOWN_TEMPLATE_BAK = "您刚和客户**%s（客户id：%s）**通完电话，该客户的匹配度：**%s**。\n" +
            "\n" +
            "### 截至本次通话已完成：\n" +
            "<font color=\"warning\">%s</font>\n" +
            "### 截至本次通话遗留事项，待下次通话解决：\n" +
            "<font color=\"info\">%s</font>\n" +
            "详细内容链接：[%s](%s)";

    public static final String LEADER_SUMMARY_MARKDOWN_TEMPLATE = "以下是您团队的半日报更新。自上次半日报至%s:\n" +
            "### 【进展】\n" +
            "<font color=\"warning\">%s</font>\n" +
            "### 【待完成】\n" +
            "<font color=\"info\">%s</font>\n" +
            "详细内容链接：[%s](%s)";


    public static final String CUSTOMER_SUMMARY_MARKDOWN_TEMPLATE = "以下是您的半日报更新。自上次半日报至%s:\n" +
            "### 【进展】\n" +
            "<font color=\"warning\">%s</font>\n" +
            "### 【待完成】\n" +
            "<font color=\"info\">%s</font>\n" +
            "详细内容链接：[%s](%s)";

    public static final String CUSTOMER_PURCHASE_TEMPLATE = "您团队的**%s**在%s与客户**%s**通完电话，今日通话总时长30分钟以上。\n" +
            "客户对购买软件的态度为：**%s**\n" +
            "%s" +
            "详细内容链接：[%s](%s)";

    public static final String PURCHASE_ATTITUDE_SUMMARY_TEMPLATE = "以下是按成交潜力划分的客户列表，供您用于跟进优先级参考:\n" +
            "1）**临门一脚**（认可数>=3且购买态度为确认购买）\n" +
            "%s" +
            "2）**还有少量卡点**（认可数>=3且购买态度为尚未确认购买）\n" +
            "%s" +
            "3）**有潜力待挖掘**（客户匹配度为中高且认可数<=2）\n" +
            "%s" +
            "详细内容链接：[%s](%s)";

    public static Set<String> staffIdList = new HashSet<>();
    public static String agentId;
    public static String corpSecret;
    public static String corpId;
    public static String accessToken;

}
