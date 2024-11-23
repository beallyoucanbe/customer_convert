package com.smart.sso.server.service;

import com.smart.sso.server.model.ActivityInfo;
import com.smart.sso.server.model.QiweiApplicationConfig;

import java.util.List;
import java.util.Map;

public interface ConfigService {
    /**
     * 获取参加这次活动的员工id
     * @return 加这次活动的员工id
     */
    List<String> getStaffIds();

    String getStaffLeader(String memberId);

    /**
     * 获取企微应用的配置
     * @return 企微
     */
    QiweiApplicationConfig getQiweiApplicationConfig();

    /**
     * 获取当前正在进行的活动Id
     * @return 当前正在进行的活动Id
     */
    String getCurrentActivityId();

    /**
     * 获取当前正在进行的活动信息
     * @return 当前正在进行的活动Id
     */
    Map<String, String> getActivityIdNames();

}
