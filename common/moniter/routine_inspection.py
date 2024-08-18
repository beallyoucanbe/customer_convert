#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : routine_instruction.py
# @Author: DingYan
# @Date  : 2018/11/6
# @Desc  :

import os
import sys
import json
import time

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../'))
from common.pyutils.sr_alarm_utils import sr_alarm
from common.pyutils.yaml_utils import read_yaml
from common.moniter.tiny_moniter import TinyMoniter
from common.moniter.moniter_conf import sr_conf, projects_conf

project_name = sys.argv[1]


class RoutineInspection(object):
    def __init__(self):
        self.tm = TinyMoniter(sr_conf, projects_conf)
        moniter_conf_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../', self.tm.company_name,
                                         project_name,
                                         'moniter_config.yaml')
        self.moniter_conf = read_yaml(moniter_conf_path)

    def run(self, module, submodule, prop, hosts):
        host_list = hosts.strip().split(',')
        start = time.strftime("%F %T")
        status, message = self.tm.do_check(module=module, submodule=submodule, prop=prop, hosts=host_list,
                                           project_name=project_name,
                                           alarm_config=self.moniter_conf[module][submodule][prop]['alarm_config'],
                                           verbose=True)
        end = time.strftime("%F %T")
        # print(status, json.dumps(message, indent=4))
        if status is True:
            level = self.moniter_conf[module][submodule][prop]['alarm_config']['alarm_level']
            title = self.moniter_conf[module][submodule][prop]['alarm_config']['alarm_title']
            title = f"{title}:[{project_name}]"
            hosts = list(message.keys())
            content = 'start_time: \t' + start + '\n' + \
                      'end_time: \t' + end + '\n' + \
                      'host:\t' + str(hosts) + '\n' + 'msg:\t' + json.dumps(message, indent=4)

            sr_alarm(level, title, content)

    def start(self):
        for module in self.moniter_conf:
            for submodule in self.moniter_conf[module]:
                for prop in self.moniter_conf[module][submodule]:
                    self.run(module, submodule, prop, self.moniter_conf[module][submodule][prop]['hosts'])

    def test(self):
        self.run(module='semoniter', submodule='semoniter', prop='url', hosts='rec01')


if __name__ == '__main__':
    # 正常解注释
    rt = RoutineInspection()
    rt.start()
