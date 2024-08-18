#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : log_tomb.py.py
# @Author: DingYan
# @Date  : 2018/12/29
# @Desc  : 在data机器上消费各rec机器上的web_service以及nginx日志，写入influxdb
import os
import sys
import time
import pickle
import socket
import logging
import json
from logging import handlers

sys.path.append(os.path.join(os.environ['SENSORS_RECOMMENDER_HOME'], 'common'))
from pyutils.sensors_recommend_config_utils import get_module_server_conf_by_key
from moniter.prometheus_helper import PrometheusHelper
from common.pyutils.sensors_recommend_config_utils import get_sr_log_path

sr_datanode_local_path = os.getenv('SENSORS_RECOMMEND_WEB_NODE_LOCAL_PATH')
nginx_log_path = get_module_server_conf_by_key(module_name='sr_nginx', key='log_dir')
LOG_FILE = nginx_log_path + '/{project_name}_access.log'
POSITION_FILE = nginx_log_path + '/{project_name}_nginx_position'

invalid_url = ['check_alive']

RESULT_FILE_SUFFIXE = '.pkl'
RESULT_FILE = nginx_log_path + '/monitor_result_tmp_{project_name}_{last_end_position}' + RESULT_FILE_SUFFIXE
RESULT_CACHE_TIME = 10

SR_LOG_PATH = get_sr_log_path()
web_service_log_path = SR_LOG_PATH + '/web_service/{project_name}'
WEB_SERVICE_LOG_FILE = web_service_log_path + '/web_service_stderr.log'
WEB_SERVICE_POSITION_FILE = web_service_log_path + '/log_check_position'
CODE_299 = '299'


def get_position(project_name, logger):
    log_file = LOG_FILE.format(project_name=project_name)
    position_file = POSITION_FILE.format(project_name=project_name)
    # 开始没position文件
    if not os.path.exists(position_file):
        logger.info('project: ' + project_name + ' position_file is not exist')
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


def backup_position(project_name, start_position, end_position):
    """

    Args:
        project_name:
        start_position:
        end_position:

    Returns:

    """
    position_file = POSITION_FILE.format(project_name=project_name)
    fh = open(position_file, 'w')
    fh.write('start_position: %s\n' % start_position)
    fh.write('end_position: %s\n' % end_position)
    fh.close()


def clear_monitor_result_log(project_name=None, backup_count=1, file_suffix=RESULT_FILE_SUFFIXE):
    """ 清理监控cache文件

    Args:
        project_name: 删除该项目下的文件
        backup_count:
        file_suffix:

    Returns:

    """
    files = []
    for f in os.listdir(nginx_log_path):
        _, suffix = os.path.splitext(f)
        if suffix == file_suffix:
            if not project_name or project_name in _:
                file = os.path.join(nginx_log_path, f)
                ctime = os.stat(file).st_ctime
                files.append((file, ctime))

    files.sort(key=lambda x: x[1], reverse=True)
    if len(files) > backup_count:
        for file, _ in files[backup_count:]:
            os.remove(file)


def handle_log(start_position, end_position, project_name, instance, logger):
    result_tmp_file = RESULT_FILE.format(project_name=project_name, last_end_position=start_position)

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
    log = open(LOG_FILE.format(project_name=project_name))
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
            if any(url_case in line for url_case in invalid_url):
                continue
            line = line.split(' ')
            http_code = line[8]
        except Exception:
            continue
        if http_code in code_dict:
            code_dict[http_code] += 1
        else:
            code_dict[http_code] = 1

        try:
            response_time_list.append(float(line[-1]))
            response_time_total += float(line[-1])
        except Exception:
            continue
        all_line += 1

    if len(response_time_list) != 0:
        response_time_list = sorted(response_time_list)
        response_time_result["response_time_average"] = round(response_time_total / all_line, 3)
        response_time_result["response_time_tp80"] = response_time_list[int(all_line * 0.8)]
        response_time_result["response_time_tp90"] = response_time_list[int(all_line * 0.9)]
        response_time_result["response_time_tp99"] = response_time_list[int(all_line * 0.99)]

    result_code_dict = handle_log_299(project_name, logger)
    if CODE_299 in result_code_dict:
        code_dict[CODE_299] = result_code_dict[CODE_299]
        code_dict['200'] -= result_code_dict[CODE_299]

    data["response_time_result"] = response_time_result
    data["code_dict"] = code_dict

    if response_time_result:
        result_tmp_file = RESULT_FILE.format(project_name=project_name, last_end_position=end_position)
        file = open(result_tmp_file, "wb")
        pickle.dump(data, file)
        file.close()
        logger.info("check nginx metric result :" + json.dumps(data))
        backup_position(project_name, start_position, end_position)

    clear_monitor_result_log(project_name=project_name)
    prometheus_helper = PrometheusHelper()
    prometheus_helper.do_monitor_nginx(instance, project_name, data)
    print(data)


## 这里来检查299的情况
def handle_log_299(project_name, logger):
    log_file = WEB_SERVICE_LOG_FILE.format(project_name=project_name)
    position_file = WEB_SERVICE_POSITION_FILE.format(project_name=project_name)
    # 开始没position文件
    if not os.path.exists(position_file):
        logger.info('project: ' + project_name + ' position_file is not exist')
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

    log = open(log_file)
    log.seek(int(start_position), 0)
    code_dict = {"200": 0}
    while True:
        current_position = log.tell()
        if current_position >= int(end_position):
            break
        line = log.readline()
        if not line:
            break
        try:
            # 这里过调掉无效数据行
            if 'REQUEST_RESULT' not in line:
                continue
            line = line.split(' ')
            # result_code 可能是299
            result_code = line[5]
        except Exception:
            continue
        if result_code in code_dict:
            code_dict[result_code] += 1
        else:
            code_dict[result_code] = 1

    print(code_dict)
    fh = open(position_file, 'w')
    fh.write('start_position: %s\n' % start_position)
    fh.write('end_position: %s\n' % end_position)
    fh.close()

    return code_dict


if __name__ == '__main__':
    project_name = sys.argv[1]
    instance = socket.gethostname()

    project_moniter_log_path = os.path.join(SR_LOG_PATH, 'moniter', project_name)
    if not os.path.exists(project_moniter_log_path):
        os.makedirs(project_moniter_log_path, mode=0o755, exist_ok=True)

    logger = logging.getLogger(project_name)
    logger.setLevel(logging.DEBUG)

    logFilePath = f"{project_moniter_log_path}/tiny_moniter.log"
    errorFilePath = f"{project_moniter_log_path}/tiny_moniter.error"

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
    logger.info('start gather online nginx metric : ' + project_name)
    start_position, end_position = get_position(project_name, logger)
    print([start_position, end_position])
    handle_log(start_position, end_position, project_name, instance, logger)
