#!/usr/bin/env python
# -*- coding: utf-8 -*-
import os
import time
import pickle
import logging
import json
from logging import handlers

from alarm import send_customer_log_alarm

log_path = '/opt/customer-convert/callback/logs'

LOG_FILE = log_path + '/log.txt'
POSITION_FILE = log_path + '/moniter/log_position'

RESULT_FILE_SUFFIXE = '.pkl'
RESULT_FILE = log_path + '/log_result_tmp_{last_end_position}' + RESULT_FILE_SUFFIXE
RESULT_CACHE_TIME = 10

def get_position(logger):
    log_file = LOG_FILE
    position_file = POSITION_FILE
    # 开始没position文件
    if not os.path.exists(position_file):
        logger.info('position_file is not exist')
        start_position = str(0)
        end_position = str(os.path.getsize(log_file))
        with open(position_file, 'w') as fh:
            fh.write('start_position: %s\n' % start_position)
            fh.write('end_position: %s\n' % end_position)

    with open(position_file) as fh:
        se = fh.readlines()
    for item in se:
        logger.info(item)
    # 万一sb了
    if len(se) != 2:
        os.remove(position_file)
        os._exit(1)
    last_start_position, last_end_position = [item.split(':')[1].strip() for item in se]
    start_position = last_end_position
    end_position = str(os.path.getsize(log_file))
    # 日志轮转发生的情况
    if int(start_position) > int(end_position):
        start_position = 0

    return map(int, [start_position, end_position])


def backup_position(start_position, end_position):
    """

    Args:
        start_position:
        end_position:

    Returns:

    """
    position_file = POSITION_FILE
    fh = open(position_file, 'w')
    fh.write('start_position: %s\n' % start_position)
    fh.write('end_position: %s\n' % end_position)
    fh.close()


def clear_monitor_result_log(backup_count=1, file_suffix=RESULT_FILE_SUFFIXE):
    """ 清理监控cache文件

    Args:
        backup_count:
        file_suffix:

    Returns:

    """
    files = []
    for f in os.listdir(log_path):
        _, suffix = os.path.splitext(f)
        if suffix == file_suffix:
            file = os.path.join(log_path, f)
            ctime = os.stat(file).st_ctime
            files.append((file, ctime))

    files.sort(key=lambda x: x[1], reverse=True)
    if len(files) > backup_count:
        for file, _ in files[backup_count:]:
            os.remove(file)


def handle_log(start_position, end_position, logger):
    result_tmp_file = RESULT_FILE.format(last_end_position=start_position)

    # 取cache信息
    if os.path.exists(result_tmp_file):
        ctime = os.stat(result_tmp_file).st_ctime
        if time.time() - ctime < RESULT_CACHE_TIME:
            file = open(result_tmp_file, "rb")
            try:
                data = pickle.load(file)
            except Exception:
                data = {}
            file.close()
            os.remove(result_tmp_file)
            if data:
                print(data)
                return
    log = open(LOG_FILE)
    log.seek(start_position, 0)
    data = {"success": 0, "error": 0}

    while True:
        current_position = log.tell()
        if current_position >= end_position:
            break
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
        result_tmp_file = RESULT_FILE.format(last_end_position=end_position)
        file = open(result_tmp_file, "wb")
        pickle.dump(data, file)
        file.close()
        logger.info("check nginx metric result :" + json.dumps(data))
        backup_position(start_position, end_position)

    clear_monitor_result_log()
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
    start_position, end_position = get_position(logger)
    print([start_position, end_position])
    handle_log(start_position, end_position, logger)
