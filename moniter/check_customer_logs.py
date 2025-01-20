#!/usr/bin/env python
# -*- coding: utf-8 -*-
import os
import time
import pickle
import logging
import json
from logging import handlers
from pathlib import Path
import mysql.connector
from datetime import datetime, timedelta

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
    data = {"weicom": 0, "telephone": 0, "success": 0, "error": 0, "event": 0}
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

    events = get_event()
    if events:
        data["event"] = events
    if data:
        logger.info("check data result :" + json.dumps(data))
        print("check data result :" + json.dumps(data))

    send_customer_log_alarm(data)


def get_event():
    events = {}
    # 替换以下变量为您的 MySQL 服务器信息
    host = "43.143.223.178"
    user = "root"
    password = "my-secret-pw"
    port = 3306
    # 创建数据库连接
    try:
        conn = mysql.connector.connect(host=host, user=user, password=password, port=port)
        # 计算昨天的日期
        yesterday = datetime.now() - timedelta(days=3)

        # 生成昨天的开始时间（00:00:00）
        start_time = yesterday.replace(hour=0, minute=0, second=0, microsecond=0)

        # 生成昨天的结束时间（23:59:59）
        end_time = yesterday.replace(hour=23, minute=59, second=59, microsecond=999999)

        # 格式化为字符串
        start_time_str = start_time.strftime('%Y-%m-%d %H:%M:%S')
        end_time_str = end_time.strftime('%Y-%m-%d %H:%M:%S')

        sql_str = f"SELECT action_type, COUNT(*) AS cnt FROM customer.events WHERE event_time >= '{start_time_str}' AND event_time <= '{end_time_str}' GROUP BY action_type"
        if conn.is_connected():
            cursor = conn.cursor()
            # 执行查询
            cursor.execute(sql_str)
            # 获取查询结果
            result = cursor.fetchall()
            if result:
                # 遍历每个数据库并打印其中的表
                for item in result:
                    events[item[0]] = item[1]
        return events

    except mysql.connector.Error as err:
        print(f"错误：{err}")

    finally:
        if conn.is_connected():
            cursor.close()
            conn.close()



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
