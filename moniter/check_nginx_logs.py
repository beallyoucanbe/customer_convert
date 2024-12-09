#!/usr/bin/env python
# -*- coding: utf-8 -*-
import os
import time
import pickle
import logging
import json
from logging import handlers
from alarm import send_nginx_log_alarm

nginx_log_path = '/var/log/nginx'
project_path = '/opt/customer-convert'

LOG_FILE = nginx_log_path + '/access.log'
POSITION_FILE = nginx_log_path + '/nginx_position'

valid_url = ['/umami']

RESULT_FILE_SUFFIXE = '.pkl'
RESULT_FILE = nginx_log_path + '/monitor_result_tmp_{last_end_position}' + RESULT_FILE_SUFFIXE
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
    for f in os.listdir(nginx_log_path):
        _, suffix = os.path.splitext(f)
        if suffix == file_suffix:
            file = os.path.join(nginx_log_path, f)
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
            if data and data.get("response_time_count"):
                print(data)
                return

    data = {}
    log = open(LOG_FILE)
    log.seek(start_position, 0)
    all_line = 0
    code_dict = {"200": 0}
    response_time_list = []
    response_time_total = 0
    response_time_result = {}

    while True:
        current_position = log.tell()
        if current_position >= end_position:
            break
        line = log.readline()
        if not line:
            break
        try:
            # 这里过调掉无效请求
            if '/umami' in line:
                continue
            line = line.split(' ')
            http_code = line[10]
        except Exception:
            continue
        if http_code in code_dict:
            code_dict[http_code] += 1
        else:
            code_dict[http_code] = 1

        try:
            response_time_list.append(float(line[9]))
            response_time_total += float(line[9])
        except Exception:
            continue
        all_line += 1

    if len(response_time_list) != 0:
        response_time_list = sorted(response_time_list)
        response_time_result["response_time_average"] = round(response_time_total / all_line, 3)
        response_time_result["response_time_tp80"] = response_time_list[int(all_line * 0.8)]
        response_time_result["response_time_tp90"] = response_time_list[int(all_line * 0.9)]
        response_time_result["response_time_tp99"] = response_time_list[int(all_line * 0.99)]

    data["response_time_result"] = response_time_result
    data["code_dict"] = code_dict

    if response_time_result:
        result_tmp_file = RESULT_FILE.format(last_end_position=end_position)
        file = open(result_tmp_file, "wb")
        pickle.dump(data, file)
        file.close()
        logger.info("check nginx metric result :" + json.dumps(data))
        backup_position(start_position, end_position)

    clear_monitor_result_log()
    send_nginx_log_alarm(data)


if __name__ == '__main__':
    log_file = LOG_FILE
    if not os.path.exists(log_file):
        os._exit(1)
    monitor_log_path = os.path.join(project_path, 'monitor')
    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)

    logFilePath = monitor_log_path + "/tiny_monitor.log"
    errorFilePath = monitor_log_path + "/tiny_monitor.error"

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
    logger.info('start nginx monitor')
    start_position, end_position = get_position(logger)
    print([start_position, end_position])
    handle_log(start_position, end_position, logger)
