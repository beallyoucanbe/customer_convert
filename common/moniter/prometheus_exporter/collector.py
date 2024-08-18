#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : base_exporter.py
# @Author: DingYan
# @Date  : 2019/9/27
# @Desc  :
import sys
import os
import json

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../'))
from moniter_conf import sr_conf, projects_conf, host_mapping, project_list
from tiny_moniter import TinyMoniter
import redis

exporter_file = '/home/sa_cluster/sr/moniter/prometheus_exporter/export_messages'


class BaseExporter(object):
    def __init__(self):
        # self.gauges = Gauge("sr", "SR modules status", ['service', 'ip', 'host_name', 'method'])
        self.tm = TinyMoniter(sr_conf, projects_conf)
        self.project_list = project_list
        self.ip_mapping = {host_mapping[host_name]: host_name for host_name in host_mapping}
        self.message = []

    def get_redis_info(self, project_name):
        """
        1. redis存活
        2.0 redis现在内存
        2.1 redis 内存占用比例
        3. client连接数量
        :return:
        """
        redis_list = self.tm.redis_biz_dict[project_name] + self.tm.redis_cache_dict[project_name]
        redis_identity_mapping = {}
        # 标记下 redis 类型， (host, port) 做标识符
        for redis_conf in self.tm.redis_biz_dict[project_name]:
            identity = str((redis_conf['host'], redis_conf['port']))
            redis_conf['identity'] = identity
            redis_identity_mapping[identity] = 'biz'
        for redis_conf in self.tm.redis_cache_dict[project_name]:
            identity = str((redis_conf['host'], redis_conf['port']))
            redis_conf['identity'] = identity
            redis_identity_mapping[identity] = 'cache'

        for redis_conf in redis_list:
            # biz 存活
            redis_conn = redis.Redis(
                host=redis_conf['host'],
                port=redis_conf['port'],
                password=redis_conf.get('password')
            )
            try:
                msg = redis_conn.ping()
            except Exception:
                msg = None
            service = 'redis_{}'.format(redis_identity_mapping[redis_conf['identity']])
            ip = redis_conf['host']
            host_name = self.ip_mapping[ip]
            method = 'survive'
            event = host_name + '_' + service + '_' + method
            if msg is True:
                value = 1
            else:
                value = 0
            self.message.append({'service': service,
                                 'ip': ip,
                                 'host_name': host_name,
                                 'method': method,
                                 'event': event,
                                 'value': value})

            # biz 内存占用
            try:
                redis_config = redis_conn.config_get()
            except Exception:
                # 就不发了
                continue
            maxmemory = redis_config['maxmemory']
            redis_info = redis_conn.info()
            used_memory = redis_info['used_memory']
            connected_clients = redis_info['connected_clients']
            self.message.append({'service': service,
                                 'ip': ip,
                                 'host_name': self.ip_mapping[ip],
                                 'method': 'maxmemory',
                                 'event': self.ip_mapping[ip] + '_' + 'redis_{}'.format(
                                     redis_identity_mapping[redis_conf['identity']]) + '_' + 'maxmemory',
                                 'value': maxmemory})

            self.message.append({'service': service,
                                 'ip': ip,
                                 'host_name': self.ip_mapping[ip],
                                 'method': 'used_memory',
                                 'event': self.ip_mapping[ip] + '_' + 'redis_{}'.format(
                                     redis_identity_mapping[redis_conf['identity']]) + '_' + 'used_memory',
                                 'value': used_memory})

            self.message.append({'service': service,
                                 'ip': ip,
                                 'host_name': self.ip_mapping[ip],
                                 'method': 'connected_clients',
                                 'event': self.ip_mapping[ip] + '_' + 'redis_{}'.format(
                                     redis_identity_mapping[redis_conf['identity']]) + '_' + 'connected_clients',
                                 'value': connected_clients})
        return

    def get_nginx_info(self, project_name):
        """
        nginx 存活
        :return:
        """
        for host in projects_conf[project_name]['nginx_address']:
            status, _ = self.tm.check_nginx(host=self.ip_mapping[host[0]], prop='survive', project_name=project_name)
            tag = 0
            if status:
                tag = 1
            self.message.append({'service': 'nginx',
                                 'ip': host[0],
                                 'host_name': self.ip_mapping[host[0]],
                                 'method': 'survive',
                                 'event': self.ip_mapping[host[0]] + '_' + 'nginx' + '_' + 'survive',
                                 'value': tag})
        return

    def get_supervisor_info(self, project_name):
        """
        1.supervisor 存活
        :return:
        """
        for host in sr_conf['exec_server_configs']['web_node']:
            status, _ = self.tm.check_supervisor(host, project_name=project_name)
            tag = 0
            if status:
                tag = 1
            self.message.append({'service': 'supervisor',
                                 'ip': sr_conf['exec_server_configs']['web_node'][host]['host'],
                                 'host_name': host,
                                 'method': 'survive',
                                 'event': host + '_' + 'supervisor' + '_' + 'survive',
                                 'value': tag})
        return

    def get_web_service_info(self, project_name):
        """
        1. web_service 存活
        :return:
        """
        for host in sr_conf['exec_server_configs']['web_node']:
            status, _ = self.tm.check_web_service(host, project_name=project_name)
            tag = 0
            if status:
                tag = 1
            self.message.append({'service': 'web_service',
                                 'ip': sr_conf['exec_server_configs']['web_node'][host]['host'],
                                 'host_name': host,
                                 'method': 'survive',
                                 'event': host + '_' + 'web_service' + '_' + 'survive',
                                 'value': tag})
        return

    def get_model_service_info(self, project_name):
        """
        1. model_service 存活
        :return:
        """
        for host in sr_conf['exec_server_configs']['web_node']:
            status, _ = self.tm.check_model_service(host, project_name=project_name)
            tag = 0
            if status:
                tag = 1
            self.message.append({'service': 'model_service',
                                 'ip': sr_conf['exec_server_configs']['web_node'][host]['host'],
                                 'host_name': host,
                                 'method': 'survive',
                                 'event': host + '_' + 'model_service' + '_' + 'survive',
                                 'value': tag})
        return

    # def generate_azkaban_info(self):
    #     """
    #     1. azkaban 存活
    #     :return:
    #     """
    #     pass

    def get_modules_info(self, module_list, project_name):
        for module in module_list:
            getattr(self, 'get_' + module + '_info')(project_name)
        return


if __name__ == '__main__':
    be = BaseExporter()
    module_list = ['nginx', 'redis', 'supervisor', 'web_service', 'model_service']
    be.get_modules_info(module_list, 'huitoutiao')
    with open(exporter_file, 'w') as f:
        json.dump(be.message, f)
