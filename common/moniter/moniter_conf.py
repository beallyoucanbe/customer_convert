#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : moniter_conf.py.py
# @Author: DingYan
# @Date  : 2019/3/12
# @Desc  : 一些tiny_moniter 需要的项目和公司的配置，和一些ansible的模板。
import os
import sys

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../'))
from common.pyutils.conf_parser import ConfParser

sr_recommend_home = os.getenv('SENSORS_RECOMMEND_HOME')
conf_parser = ConfParser()
projects_conf = conf_parser.projects_conf
sr_conf = conf_parser.sr_conf
project_list = list(projects_conf.keys())
host_dict = sr_conf["exec_server_configs"].get('data_node', {})
host_dict.update(sr_conf["exec_server_configs"].get('web_node', {}))
host_mapping = {host_name: host_dict[host_name]['host'] for host_name in host_dict}
ip_mapping = {host_mapping[host_name]: host_name for host_name in host_mapping}
