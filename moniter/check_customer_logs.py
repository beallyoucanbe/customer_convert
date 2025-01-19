#!/usr/bin/env python
# -*- coding: utf-8 -*-
import os
import time
import datetime
import pickle
import logging
import json
from logging import handlers
from pathlib import Path

from alarm import send_customer_log_alarm

call_back_file_path = '/data/customer-convert/callback/'
call_back_file_path_wecom = call_back_file_path + 'wecom/'
call_back_file_path_telephone = call_back_file_path + 'telephone/'
log_path = '/data/customer-convert/callback/logs'

LOG_FILE = log_path + '/log.txt'

def handle_log(logger):
    today = datetime.date.today()
    formatted_date = today.strftime('%Y-%m-%d')
    # 检查有多少个微信文件
    data = {"weicom": 0, "telephone": 0, "success": 0, "error": 0}
    wecom_folder = call_back_file_path_wecom + formatted_date
    folder_path = Path(wecom_folder)
    if folder_path.is_dir():
        file_count = 0
        for item in folder_path.iterdir():
            # 检查是否为文件
            if item.is_file():
                file_count += 1
        data['weicom'] = file_count
    # 检查有多少个语音文件
    file_path = Path(call_back_file_path_telephone + formatted_date + "_message.txt")
    if file_path.is_file():
        # 初始化行数计数器
        line_count = 0
        # 打开文件并逐行读取
        with file_path.open('r', encoding='utf-8') as file:
            for line in file:
                line_count += 1
        data['telephone'] = line_count

    # 检查events表中各个event的数量
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
            elif "mysql dict is" in line:
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
