#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : sr_zk_utils.py
# @Author: DingYan
# @Date  : 2020/3/26
# @Desc  : 从sa zk读取sr各组件所需配置
import os
import sys

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../../'))

from hyperion_element.global_properties import GlobalProperties
from hyperion_guidance.zk_connector import ZKConnector

zk_connector = ZKConnector()
global_properties = GlobalProperties()

ZK_HOME_OLD = '/deploy'
ZK_HOME_NEW = 'sr/install'


def set_config(relativepath, content):
    """
    用zkroot的相对路径设置配置，如果没有目录创建目录
    :param relativepath:
    :param content:
    :return:
    """
    zk_connector.set_json_value_by_path(relativepath, content)


def set_common_config(content):
    """
    设置common配置
    :param content:
    :return:
    """
    relativepath = os.path.join(ZK_HOME_NEW, 'sr')
    set_config(relativepath, content)


def get_common_config():
    """
    获取common配置
    :return:
    """
    relativepath = os.path.join(ZK_HOME_NEW, 'sr')
    return zk_connector.get_json_value_by_path(relativepath)


def check_module(module_name):
    relativepath = os.path.join(ZK_HOME_NEW, 'sr', module_name)
    return zk_connector.check_config_by_path(relativepath)


def set_module_config(module_name, content):
    """
    设置模块的配置
    :return:
    """
    relativepath = os.path.join(ZK_HOME_NEW, 'sr', module_name)
    set_config(relativepath, content)


def get_module_config(module_name):
    """
    获取模块的配置
    :param module_name:
    :return:
    """
    relativepath = os.path.join(ZK_HOME_NEW, 'sr', module_name)
    return zk_connector.get_json_value_by_path(relativepath)


def check_role(module_name, role_name):
    relativepath = os.path.join(ZK_HOME_NEW, 'sr', module_name, role_name)
    return zk_connector.check_config_by_path(relativepath)


def set_role_config(module_name, role_name, content):
    """
    设置角色配置
    :param module_name:
    :param role_name:
    :param content:
    :return:
    """
    relativepath = os.path.join(ZK_HOME_NEW, 'sr', module_name, role_name)
    set_config(relativepath, content)


def get_role_config(module_name, role_name):
    """
    设置角色配置
    :param module_name:
    :param role_name:
    :return:
    """
    relativepath = os.path.join(ZK_HOME_NEW, 'sr', module_name, role_name)
    return zk_connector.get_json_value_by_path(relativepath)


def get_module_list():
    """
    获取全部模块
    :return:[]
    """
    product_path = os.path.join(global_properties.zookeeper.root_path, ZK_HOME_NEW, 'sr')
    if zk_connector.exists(product_path):
        return zk_connector.get_children_by_path(product_path)
    else:
        return []


def get_role_list(module_name):
    module_path = os.path.join(global_properties.zookeeper.root_path, ZK_HOME_NEW, 'sr', module_name)
    if zk_connector.exists(module_path):
        return zk_connector.get_children_by_path(module_path)
    else:
        return []


def get_module_map():
    '''
    获取模块列表 是个三层dict
    {'product_a': {'module_a': {'role_a': ['host_a:port_a', 'host_b:port_b']}}}
    '''
    root_path = os.path.join(global_properties.zookeeper.root_path, ZK_HOME_NEW)
    product = 'sr'
    ret = {}
    if zk_connector.exists(os.path.join(root_path, product)):
        for module in zk_connector.get_children_by_path(os.path.join(root_path, product)):
            ret[module] = {}
            for role in zk_connector.get_children_by_path(os.path.join(root_path, product, module)):
                ret[module][role] = \
                    zk_connector.get_children_by_path(os.path.join(root_path, product, module, role))
    return ret


def get_roles_config_dict(module_name):
    """
    获取模块下所有role的conf
    :param module_name:
    :return:
    """
    return {role: get_role_config(module_name, role) for role in get_role_list(module_name)}


def get_all_module_config_dict():
    """
    获取sr全部模块的配置，返回dict

    :return: {
        module_a:{},
        module_b:{}
    }
    """
    module_list = get_module_list()
    return {module: get_module_config(module) for module in module_list}
