#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : routine_instruction.py
# @Author: DingYan
# @Date  : 2018/11/6
# @Desc  : 该脚本是项目级别的指标收集脚本，部署时，需要针对需要监控的项目启动一个收集任务：
# 例子：*/1 * * * * source /home/sa_cluster/.bash_profile ;/sensorsdata/main/program/sr/bin/sr_python /sensorsdata/main/program/sr/common/moniter/metric_monitor.py production__1__app

import os
import sys

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../'))
from common.pyutils.yaml_utils import read_yaml
from common.moniter.gather_metrics import GatherMetrics
from common.moniter.moniter_conf import sr_conf, projects_conf

project_name = sys.argv[1]


class MetricMonitor(object):
    def __init__(self):
        self.gather_metric = GatherMetrics(sr_conf, projects_conf)
        moniter_conf_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'moniter_config.yaml')
        self.moniter_conf = read_yaml(moniter_conf_path)

    def run(self, module, submodule):
        self.gather_metric.do_check(module=module, submodule=submodule, project_name=project_name)

    def start(self):
        for module in self.moniter_conf:
            for submodule in self.moniter_conf[module]:
                self.run(module, submodule)


if __name__ == '__main__':
    # 正常解注释
    mm = MetricMonitor()
    mm.start()
