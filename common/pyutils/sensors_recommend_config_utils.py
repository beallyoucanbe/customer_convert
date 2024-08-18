#! --*-- coding: utf-8 --*--

__author__ = "songming"

import os
from importlib.machinery import SourceFileLoader

from hyperion_client.config_manager import ConfigManager

SENSORS_DATA_HOME = '/home/sa_cluster'
SENSORS_RECOMMENDER_HOME = '/home/sa_cluster/sr'
SENSORS_ANALYTICS_HOME = '/home/sa_cluster/sa'
SENSORS_PLATFORM_HOME = '/home/sa_cluster/sp'
SENSORS_RECOMMEND_WEB_NODE_DATA_PATH = '/data/sr_cluster'


def get_sd_home_path():
    return os.getenv('SENSORS_DATA_HOME', SENSORS_DATA_HOME)


def get_sp_home_path():
    return os.getenv('SENSORS_PLATFORM_HOME', SENSORS_PLATFORM_HOME)


def get_sa_home_path():
    return os.environ.get("SENSORS_ANALYTICS_HOME", SENSORS_ANALYTICS_HOME)


def get_sr_home_path():
    return os.environ.get("SENSORS_RECOMMENDER_HOME", SENSORS_RECOMMENDER_HOME)


def get_python_cmd():
    home_path = get_sr_home_path()
    cmd = os.path.join(home_path, "python/bin/python3.6")
    return cmd


def get_sr_log_path():
    return os.environ.get("SENSORS_RECOMMENDER_LOG_DIR")


def get_sr_conf_path():
    company_name = ConfigManager().get_product_global_conf_by_key('sr', 'company_name')
    sr_path = get_sr_home_path()
    path = os.path.join(sr_path, company_name, 'conf', 'sensors_recommend_conf.py')
    return path


def get_sr_conf():
    path = get_sr_conf_path()
    sr_conf = SourceFileLoader('sr_conf', path).load_module()
    return sr_conf


def get_company_name():
    sr_conf = get_sr_conf()
    print(dir(sr_conf))
    return sr_conf.sensors_recommend_company_name


def get_project_list():
    sr_conf = get_sr_conf()
    return sr_conf.sensors_recommend_project_names


def get_module_client_conf_by_key(module_name: str, key: str):
    """
    获取模块的client配置中某个key的值
    :param module_name:
    :param key:
    :return:
    """
    return ConfigManager().get_client_conf_by_key(product_name='sr',
                                                  module_name=module_name,
                                                  key=key)


def get_module_server_conf_by_key(module_name: str, key: str):
    """
    获取模块的server配置中某个key的值
    :param module_name:
    :param key:
    :return:
    """
    return ConfigManager().get_server_conf_by_key(product_name='sr',
                                                  module_name=module_name,
                                                  key=key)
