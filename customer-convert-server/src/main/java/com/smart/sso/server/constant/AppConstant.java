package com.smart.sso.server.constant;

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

    public static final String REFRESH_CUSTOMER_COMPLETE_DESCRIBE = "REFRESH_CUSTOMER_COMPLETE_DESCRIBE";

    public static final String SOURCEID_KEY_PREFIX = "processed:";


    public static final String CUSTOMER_SUMMARY_MARKDOWN_TEMPLATE = "您刚和客户**%s（客户id：%s）**通完电话，该客户的匹配度：**%s**。\n" +
            "\n" +
            "### 截至本次通话已完成：\n" +
            "<font color=\"warning\">%s</font>\n" +
            "### 截至本次通话遗留事项，待下次通话解决：\n" +
            "<font color=\"info\">%s</font>\n" +
            "详细内容链接：[%s](%s)";

    public static final String LEADER_SUMMARY_MARKDOWN_TEMPLATE = "以下是您团队的半日报更新。截至%s:\n" +
            "### 【问题】\n" +
            "<font color=\"info\">%s</font>\n" +
            "### 【进展】\n" +
            "<font color=\"warning\">%s</font>\n" +
            "详细内容链接：[%s](%s)";
}
