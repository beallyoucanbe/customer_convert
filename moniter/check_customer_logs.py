#!/usr/bin/env python
# -*- coding: utf-8 -*-
import os
import logging
import json
from logging import handlers
from alarm import send_customer_log_alarm

log_path = '/home/haiyangu1/hsw/logs'
LOG_FILE = log_path + '/log.txt'

def handle_log(logger):
    data = {"success": 0, "error": 0}
    # 检查大模型的处理日志
    log = open(LOG_FILE)
    while True:
        line = log.readline()
        if not line:
            break
        try:
            # 这里过调掉无效请求
            if "ERROR" in line:
                data["error"] = data["error"] + 1
            elif "mysql" in line:
                data["success"] = data["success"] + 1
        except Exception:
            continue
    if data:
        logger.info("check data result :" + json.dumps(data))
        print("check data result :" + json.dumps(data))
    send_customer_log_alarm(data)


if __name__ == '__main__':
    log_file = LOG_FILE
    if not os.path.exists(log_file):
        os._exit(1)
    moniter_log_path = os.path.join(log_path, 'moniter')
    if not os.path.exists(moniter_log_path):
        os.makedirs(moniter_log_path, mode=0o755, exist_ok=True)

    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)

    logFilePath = moniter_log_path + "tiny_moniter.log"
    errorFilePath = moniter_log_path + "tiny_moniter.error"

    fa = handlers.RotatingFileHandler(logFilePath, 'a', 1024 * 1024, 10)
    fa.setLevel(logging.INFO)
    formater = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    fa.setFormatter(formater)
    logger.addHandler(fa)

    fe = handlers.RotatingFileHandler(errorFilePath, 'a', 1024 * 1024, 10)
    fe.setLevel(logging.ERROR)
    formater = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    fe.setFormatter(formater)
    logger.addHandler(fe)

    logger.info('start customer log monitor')
    handle_log(logger)
