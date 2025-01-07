package com.smart.sso.server.service.impl;

import com.smart.sso.server.service.CommunicationService;
import com.smart.sso.server.util.ShellUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CommunicationServiceImpl implements CommunicationService {
    @Override
    @Async
    public void wecomCallBack(String filePath) {
        try {
            // python process_text.py file /data/customer-convert/callback/wecom/2025-01-06/3033_3680785_18_39_26
            String[] params = {"file", filePath};
            Process process = ShellUtils.saPythonRun("/home/csuser/hsw/chat_insight_v2/dads/hezhong/process_text.py", params.length, params);
            // 等待脚本执行完成
            int exitCode = process.waitFor();
            log.error("Python脚本执行完成，退出码：" + exitCode);
        } catch (Exception e) {
            // 这里只负责调用对用的脚本
            log.error("执行脚本报错", e);
        }

    }

    @Override
    public void telephoneCallBack(String filePath) {
        try {
            // python process_text.py call '{"sales_id":5155,"sales_name":"张鹏","customer_id":3700105,"customer_name":"陈炜鹏","task_id":"02b0711f911f4a72a126eb28d08d02f2","chat_start_time":"2024-12-25T15:37:02+08:00"}'            String[] params = {filePath};
            String[] params = {"call", "'" + filePath + "'"};
            Process process = ShellUtils.saPythonRun("/home/csuser/hsw/chat_insight_v2/dads/hezhong/process_text.py", params.length, params);
            // 等待脚本执行完成
            int exitCode = process.waitFor();
            log.error("Python脚本执行完成，退出码：" + exitCode);
        } catch (Exception e) {
            // 这里只负责调用对用的脚本
            log.error("执行脚本报错", e);
        }
    }
}
