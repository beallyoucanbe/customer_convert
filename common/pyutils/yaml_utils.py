#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : yaml_utils.py.py
# @Author: DingYan dingyan@sensorsdata.cn
# @Date  : 2020/8/27
# @Desc  : sr统一的yaml读写接口
import os
import yaml


def read_yaml(yml_file):
    """
    读取yaml转成python对应对象
    :param yml_file: yaml文件路径
    :return: 合适的python对象
    """
    if not os.path.isfile(yml_file):
        return None
    with open(yml_file, 'r') as f:
        return yaml.load(f)


def write_conf_to_yml_file(conf, yml_file, default_flow_style=None):
    """
    将conf(dict)写到yaml文件
    :param conf: dict
    :param yml_file: yml文件路径
    :param default_flow_style: dump的风格
    :return: None
    """
    with open(yml_file, 'w+') as f:
        yaml.dump(conf, f, default_flow_style=default_flow_style)
