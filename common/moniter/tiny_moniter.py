#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : tiny_moniter.py.py
# @Author: DingYan
# @Date  : 2019/3/8
# @Desc  : tiny_moniter v1.1

import os
import sys
import logging
from logging import handlers
import json
import traceback

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../'))
from common.moniter.ansible_helper import AnsibleHelper
from common.moniter.moniter_conf import project_list
from common.moniter.semoniter import Semoniter
from common.pyutils.sensors_recommend_config_utils import get_sr_home_path, get_sr_log_path

SR_HOME_PATH = get_sr_home_path()
SR_LOG_PATH = get_sr_log_path()
INVENTORY = f'{SR_HOME_PATH}/moniter/hosts'


class TinyMoniter(object):
    def __init__(self, sr_conf, projects_conf):
        # 初始化各种conf的配置信息
        self.company_name = sr_conf.get('company_name')
        self.sr_conf = sr_conf
        self.project_conf = projects_conf
        self.project_list = project_list

        # 创建ansible_client
        self.ansible_client = AnsibleHelper(inventory=INVENTORY)
        # 创建各个项目的logger
        for project in project_list:
            project_moniter_log_path = os.path.join(SR_LOG_PATH, 'moniter', project)
            if not os.path.exists(project_moniter_log_path):
                os.makedirs(project_moniter_log_path, mode=0o755, exist_ok=True)
            setattr(self, project + '_logger', logging.getLogger(project))
            logger = getattr(self, project + '_logger')
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

    @staticmethod
    def neq(a, b):
        if a is None:
            return True
        if a != b:
            return True
        return False

    @staticmethod
    def equal(a, b):
        if a is None:
            return True
        if a == b:
            return True
        return False

    @staticmethod
    def above(a, b):
        if a is None:
            return True
        if a > b:
            return True
        return False

    @staticmethod
    def below(a, b):
        if a is None:
            return True
        if a < b:
            return True
        return False

    def do_moniter(self, module, submodule, prop, hosts, project_name, alarm_config=None, verbose=False):
        """获取对应监控项信息

        Args:
            module:
            submodule:
            prop:
            hosts:
            project_name:
            alarm_config:
            verbose:

        Returns:

        """
        hosts = [i.strip() for i in hosts]
        try:
            output = getattr(self, 'get_' + module + '_msg')(submodule=submodule, prop=prop, hosts=hosts,
                                                             project_name=project_name, alarm_config=alarm_config)
            getattr(self, project_name + '_logger').info(
                'module: ' + module + ' submodule: ' + submodule + ' prop: ' + prop + ' output: \n' + str(output))
        except Exception:
            output = {host: (None, traceback.format_exc()) for host in hosts}
            getattr(self, project_name + '_logger').error(
                'module: ' + module + ' submodule: ' + submodule + ' prop: ' + prop + ' output: \n' + str(output))
        if verbose:
            print('module: ' + module + ' submodule: ' + submodule + ' prop: ' + prop + ' output: \n' + str(output))
        return output

    def jugde_alarm(self, message_dict, alarm_config):
        """


        :param message_dict: {rec01:(value,msg)}
        :param alarm_config: {'alarm_level': 'fatal',
                             'alarm_title': 'SR_nginx_log_abnormal',
                             'alarm_threhold': ['above', 0.005]     # [方法, 值]
                             }
        :return: state: True or False 是否需要报警
                message: 报警信息
        """
        message = {}
        alarm_threhold = alarm_config["alarm_threhold"]
        for host, host_message in message_dict.items():
            state = getattr(self, alarm_threhold[0])(host_message[0], alarm_threhold[1])
            if state is True:
                message[host] = host_message
        if message:
            tag = True
        else:
            tag = False
        return tag, message

    def do_check(self, module, submodule, prop, hosts, project_name, alarm_config, verbose=False):
        """
        检查对应监控项， 先从获取监控信息，然后结合报警配置返回是否需要报警和报警附带的信息
        :param module 模块名
        :param submodule 子模块
        :param prop 监控属性
        :param host [] 需要检查的机器
        :param project_name 项目名
        :param alarm_config 报警配置
        :param verbose 是否打在consle
        :return:
            state: 监控返回结果 成功 True， 失败 False
            output:返回的报警附带信息

        """
        if project_name not in self.project_list:
            return False, 'Project is not exist'
        # 获取监控项返回信息
        message_dict = self.do_moniter(module=module, submodule=submodule, prop=prop, hosts=hosts,
                                       project_name=project_name, alarm_config=alarm_config)
        state, message = self.jugde_alarm(message_dict=message_dict, alarm_config=alarm_config)
        # 将获取的信息结合报警配置返回给调度器
        # 打日志
        if verbose:
            print('module: ' + module + ' submodule: ' + submodule + ' prop: ' + prop + ' state:\n' + str(state))
        # 触发报警时 打日志
        if state is True:
            getattr(self, project_name + '_logger').error(
                'module: ' + module + ' submodule: ' + submodule + ' prop: ' + prop + ' message: \n' + str(message))
        return state, message

    def get_flink_job_msg(self, hosts, submodule, prop, **kwargs):
        output_dict = {}
        if prop not in ['survive']:
            return {host: (None, "Invalid prop") for host in hosts}
        elif prop == 'survive':
            state, output = self.ansible_client.shell(host=hosts, user='sa_cluster',
                                                      cmd="source ~/.bashrc && yarn application -list | tail -n +3| awk '{print $2, $7}'")  # noqa
            for host in output:
                output_dict[host] = (0, output[host]["stdout_lines"])
                tag = False
                for line in output[host]["stdout_lines"]:
                    if (submodule in line) and ('RUNNING' in line):
                        tag = True
                if tag:
                    output_dict[host] = (1, output[host]["stdout_lines"])
            return output_dict

    def get_semoniter_msg(self, hosts, project_name, **kwargs):
        sys.path.append(
            os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../', self.company_name, project_name, 'conf',
                         'semoniter'))
        from semoniter_conf import case_dict
        with open(
                os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../', self.company_name, project_name,
                             'conf',
                             'semoniter', 'test_cases.json'), 'r') as jsonfile:
            case_conf = json.load(jsonfile)
        sm = Semoniter(case_dict, case_conf)
        for case, request in sm.case_list:
            state, output = self.ansible_client.shell(host=hosts, user='sa_cluster', cmd=request)
            for host in hosts:
                try:
                    sm.set_case_return(case, request, output[host]["stdout_lines"][0])
                except Exception:
                    getattr(self, project_name + '_logger').error(
                        'module: semoniter' + ' submodule: semoniter' + ' prop: url' + f' request: {request}' +
                        ' output: \n' + str(output))
        status, message = sm.verify()
        if status is True:
            return {str(hosts): (1, message)}
        return {str(hosts): (0, message)}
