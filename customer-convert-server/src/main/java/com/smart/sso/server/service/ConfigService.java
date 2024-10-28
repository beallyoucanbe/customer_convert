package com.smart.sso.server.service;

import java.util.List;

public interface ConfigService {
    /**
     * 获取参加这次活动的员工id
     * @return
     */
    List<String> getStaffIds();
}
