#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : routine_instruction.py
# @Desc  :  该脚本是环境级别的指标收集脚本，部署时，一个sr 集群只需要启动一个任务
# 例子：*/1 * * * * source /home/sa_cluster/.bash_profile ;/sensorsdata/main/program/sr/bin/sr_python /sensorsdata/main/program/sr/common/moniter/global_metric_monitor.py

import os
import sys
import logging
from logging import handlers
from hyperion_client.deploy_topo import DeployTopo

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../'))

from common.moniter.gather_metrics import GatherMetrics
from common.moniter.azkaban_task_monitor import exec_exector_flow_failed_monitor
from common.moniter.moniter_conf import sr_conf, projects_conf
from common.pyutils.sensors_recommend_config_utils import get_sr_log_path

SR_LOG_PATH = get_sr_log_path()


def get_install_logger():

    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)
    moniter_log_path = os.path.join(SR_LOG_PATH, 'moniter')
    logFilePath = f"{moniter_log_path}/tiny_moniter.log"
    errorFilePath = f"{moniter_log_path}/tiny_moniter.error"

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
    return logger


class GlobalMetricMonitor(object):

    def __init__(self):
        self.gather_metric = GatherMetrics(sr_conf, projects_conf)
        self.logger = get_install_logger()

    def start(self):
        # 执行检查azkaban 的任务
        self.logger.info("start check azkaban dataflow status")
        exec_exector_flow_failed_monitor(self.logger)
        # 执行sp nginx检查
        dp = DeployTopo()
        hosts = dp.get_host_list_by_role_group_name('meta')
        self.gather_metric.get_sp_nginx_msg(hosts)


if __name__ == '__main__':
    # 正常解注释
    mm = GlobalMetricMonitor()
    mm.start()
