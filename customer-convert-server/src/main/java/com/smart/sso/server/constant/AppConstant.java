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

    public static final String REFRESH_CUSTOMER_COMPLETE_DESCRIBE = "REFRESH_CUSTOMER_COMPLETE_DESCRIBE";


    public static final String CUSTOMER_SUMMARY_MARKDOWN_TEMPLATE = "您刚和客户 %s 通完电话，该客户的匹配度：**%s**。\n" +
            "\n" +
            "### 本次通话已完成：\n" +
            "%s\n" +
            "### 本次通话遗留问题，待下次通话解决：\n" +
            "%s\n" +
            "详细内容链接：[%s](%s)";
}
