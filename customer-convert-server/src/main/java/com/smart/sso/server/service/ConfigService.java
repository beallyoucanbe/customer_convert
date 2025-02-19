package com.smart.sso.server.service;

import com.smart.sso.server.model.QiweiApplicationConfig;

import java.util.List;
import java.util.Map;

public interface ConfigService {
    /**
     * 获取参加这次活动的员工id
     * @return 分区域加这次活动的员工id
     */
    Map<String, List<String>> getStaffIds();

    Map<String, String> getStaffLeaderMap();

    Map<String, String> getStaffManagerMap();

    Map<String, String> getUserIdNamerMap();

    /**
     * 获取参加这次活动的员工id
     * @return 分领导加这次活动的员工id
     */
    Map<String, List<String>> getStaffIdsLeader();

    String getStaffAreaRobotUrl(String memberId);

    /**
     * 获取企微应用的配置
     * @return 企微
     */
    Map<String, QiweiApplicationConfig> getQiweiApplicationConfig();

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

    Map<String, String> getRobotMessageUrl();

    void refreshCustomerConfig();

}
