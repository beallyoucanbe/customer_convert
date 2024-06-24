package com.smart.sso.server.constant;

import java.util.Arrays;
import java.util.List;

/**
 * 应用常量
 *
 * @author Joe
 */
public class AppConstant {

    // 存cookie中TGT名称，和Cas保存一致
    public static final String TGC = "TGC";

    // 登录页
    public static final String LOGIN_PATH = "/login";

    public static final String REFRESH_CONVERSION_RATE = "REFRESH_CONVERSION_RATE";

    public static final List<String> fundsVolume = Arrays.asList("充裕", "匮乏", "大于等于10万", "小于10万");

    public static final List<String> earningDesire = Arrays.asList("高", "低");


}
