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
import redis
import datetime
import json
import subprocess

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../'))
from common.moniter.ansible_helper import AnsibleHelper
from common.moniter.moniter_conf import project_list, ip_mapping
from common.pyutils.sensors_recommend_config_utils import get_sr_home_path, get_sr_log_path
from common.moniter.prometheus_helper import PrometheusHelper
from sr_monitor_db_utils import save_sr_monitor_to_db

SR_HOME_PATH = get_sr_home_path()
SR_LOG_PATH = get_sr_log_path()
INVENTORY = f'{SR_HOME_PATH}/moniter/hosts'

class ByteToStringEncoder(json.JSONEncoder):

    def default(self, obj):
        """
        判断是否为bytes类型的数据是的话转换成str
        :param obj:
        :return:
        """
        if isinstance(obj, bytes):
            return str(obj, encoding='utf-8')
        return json.JSONEncoder.default(self, obj)


class GatherMetrics(object):
    def __init__(self, sr_conf, projects_conf):
        # 初始化各种conf的配置信息
        self.company_name = sr_conf.get('company_name')
        self.sr_conf = sr_conf
        self.project_conf = projects_conf
        self.project_list = project_list

        self.prometheus_helper = PrometheusHelper()

        def trans_redis_conf_to_dict(confs):
            """ [('10.42.22.215', 6501)] -> [{'host': '10.42.22.215', 'port': 6501, 'password': 'MhxzKhl2015'}]
            :param confs:
            :return:
            """
            rs = []
            for conf in confs:
                if not isinstance(conf, dict):
                    conf = {'host': conf[0], 'port': conf[1], 'password': None}
                rs.append(conf)
            return rs

        self.redis_biz_dict = {}
        self.redis_cache_dict = {}
        self.web_nginx_dict = {}
        for x in self.project_list:
            redis_biz = self.project_conf[x]['redis_server_configs']['biz']
            redis_cache = self.project_conf[x]['redis_server_configs']['cache']

            self.redis_biz_dict[x] = trans_redis_conf_to_dict(redis_biz)
            self.redis_cache_dict[x] = trans_redis_conf_to_dict(redis_cache)
            self.web_nginx_dict[x] = self.project_conf[x]['nginx_address']

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

    def do_moniter(self, module, submodule, project_name):
        """获取对应监控项信息

        Args:
            module:
            submodule:
            project_name:
        Returns:

        """
        try:
            output = getattr(self, 'get_' + module + '_msg')(submodule=submodule, project_name=project_name)
            getattr(self, project_name + '_logger').info(
                'module: ' + module + ' submodule: ' + submodule + ' output: \n' + str(output))
        except Exception:
            getattr(self, project_name + '_logger').error(
                'module: ' + module + ' submodule: ' + submodule + ' output: \n' + str(output))
        return output

    def do_check(self, module, submodule, project_name):
        """
        检查对应监控项， 先从获取监控信息，然后结合报警配置返回是否需要报警和报警附带的信息
        :param module 模块名
        :param submodule 子模块
        :param project_name 项目名
        :return:
            state: 监控返回结果 成功 True， 失败 False
            output:返回的报警附带信息

        """
        if project_name not in self.project_list:
            return False, 'Project is not exist'
        self.do_moniter(module=module, submodule=submodule, project_name=project_name)

    def get_nginx_msg(self, submodule, project_name):
        """
        监控nginx的信息，将各机器的值汇总返回，出错打报错日志但是不会抛出
        :param project_name:
        :return: output_dict{}
        """
        output_dict = {}
        hosts = []
        for conf in self.web_nginx_dict[project_name]:
            hosts.append(ip_mapping[conf[0]])
        cmd = 'source /home/sa_cluster/.bashrc && ' + \
              f'{SR_HOME_PATH}/bin/sr_python {SR_HOME_PATH}/common/moniter/moniter_tools/check_nginx_logs.py {project_name}'  # noqa
        state, output = self.ansible_client.shell(host=hosts, user='sa_cluster', cmd=cmd)
        print("state", state)
        print("output", output)
        return output_dict

    def get_azkaban_msg(self, submodule, project_name):
        """
        监控azkaban 的任务情况
        :param project_name:
        :return: output_dict{}
        """
        ## azkaban 5分钟检查一次
        output_dict = {}
        cmd = 'source /home/sa_cluster/.bashrc && ' + \
              f'{SR_HOME_PATH}/bin/sr_python {SR_HOME_PATH}/common/moniter/azkaban_task_monitor.py {project_name} {submodule}'  # noqa
        if datetime.datetime.now().minute % 5 == 0:
            subprocess.check_call(cmd, shell=True)
        return output_dict

    def get_sp_nginx_msg(self, hosts):
        """
        监控sp nginx的信息，将各机器的值汇总返回，出错打报错日志但是不会抛出
        :param project_name:
        :return: output_dict{}
        """
        output_dict = {}
        cmd = 'source /home/sa_cluster/.bashrc && ' + \
              f'{SR_HOME_PATH}/bin/sr_python {SR_HOME_PATH}/common/moniter/moniter_tools/check_sp_nginx_logs.py'  # noqa
        state, output = self.ansible_client.shell(host=hosts, user='sa_cluster', cmd=cmd)
        print("state", state)
        print("output", output)
        return output_dict

    def get_redis_msg(self, submodule, project_name):
        output_dict = {}
        instance_redis_info_dict = {}
        for redis_member in getattr(self, 'redis_' + submodule + '_dict')[project_name]:
            if not isinstance(redis_member, dict):
                redis_member = {'host': redis_member[0], 'port': redis_member[1], 'password': None}
            redis_conn = redis.Redis(
                host=redis_member['host'],
                port=redis_member['port'],
                password=redis_member.get('password')
            )
            try:
                redis_info = redis_conn.info()
            except Exception as e:
                msg = str(e)
                output_dict[redis_member['host']] = (None, msg)
                continue
            instance = ip_mapping[redis_member['host']]
            instance_redis_info_dict[instance] = redis_info
            ## 这里检查slow log， 每小时检查一次
            if datetime.datetime.now().minute == 1:
                slow_log_len = redis_conn.slowlog_len()
                if slow_log_len > 0:
                    slow_log_info = redis_conn.slowlog_get(slow_log_len)
                    result = ",".join(json.dumps(item, ensure_ascii=False, cls=ByteToStringEncoder, indent=4) for item in slow_log_info)
                    ## 处理 slow log, mysql text 最多存储65535 个字符
                    if len(result) > 65534:
                        result = result[0:65534]
                    type = 'redis_' + submodule
                    save_sr_monitor_to_db(project_name, type, 'slow_log', instance, result)
                    ## 处理完之后重置slow log
                    redis_conn.slowlog_reset()

        self.prometheus_helper.do_monitor_redis(project_name, instance_redis_info_dict, submodule)
        return output_dict
