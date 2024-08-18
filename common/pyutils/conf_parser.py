# coding: utf-8

import os
from collections import ChainMap
from importlib.machinery import SourceFileLoader

from .sensors_recommend_config_utils import get_sr_conf_path


class ConfParser(object):
    def __init__(self, sr_conf_path=None):
        self.sr_conf_path = sr_conf_path if sr_conf_path else get_sr_conf_path()

        self.sr_conf, self.sr_obj = self._parse_sr_conf(self.sr_conf_path)
        self.projects_conf = self._parse_project_conf()

    def _parse_sr_conf(self, sr_conf_path):
        sr_conf_object = SourceFileLoader('sr_conf_object', sr_conf_path).load_module()
        sr_conf_dict = {
            'company_name': sr_conf_object.sensors_recommend_company_name,
            'project_names': sr_conf_object.sensors_recommend_project_names,

            'home_path': sr_conf_object.sensors_recommend_home,
            'data_node_data_path': sr_conf_object.sensors_recommend_data_node_data_path,
            'web_node_data_path': sr_conf_object.sensors_recommend_web_node_data_path,
            'hdfs_data_path': sr_conf_object.sensors_recommend_hdfs_data_path,

            'exec_server_configs': sr_conf_object.sensors_recommend_exec_server_configs,
        }

        sr_conf_dict['exec_server_identity_dict'] = \
            {node: identity for identity, nodes in sr_conf_dict['exec_server_configs'].items() for node in nodes}
        sr_conf_dict['exec_server_connection_configs'] = ChainMap(*sr_conf_dict['exec_server_configs'].values())

        sr_conf_dict['company_path'] = os.path.join(sr_conf_dict['home_path'], sr_conf_dict['company_name'])
        sr_conf_dict['sr_python'] = os.path.join(sr_conf_dict['home_path'], 'python', 'bin', 'python3')
        sr_conf_dict['sr_python2'] = os.path.join(sr_conf_dict['home_path'], 'python2', 'bin', 'python2.7')

        # 补充一些company级别的conf

        return sr_conf_dict, sr_conf_object

    def _parse_project_conf(self):
        projects_conf = {}
        for project_name in self.sr_conf['project_names']:
            project_conf_path = os.path.join(self.sr_conf['home_path'], self.sr_conf['company_name'],
                                             project_name, 'conf', 'sensors_recommend_project_conf.py')
            project_conf = SourceFileLoader('project_conf', project_conf_path).load_module()
            projects_conf[project_name] = {
                'project_name': project_name,
                'redis_server_configs': project_conf.sensors_recommend_redis_server_configs,
                'nginx_address': project_conf.sensors_recommend_nginx_addresses,
                'model_service_address': project_conf.sensors_recommend_model_service_addresses
            }
        return projects_conf
