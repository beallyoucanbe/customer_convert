package com.smart.sso.server.service;

import java.util.List;

public interface ConfigService {
    /**
     * 获取参加这次活动的员工id
     * @return 加这次活动的员工id
     */
    List<String> getStaffIds();

    /**
     * 获取当前正在进行的活动Id
     * @return 当前正在进行的活动Id
     */
    String getCurrentActivityId();
}
