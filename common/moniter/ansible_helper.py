#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : s_ansible.py.py
# @Author: DingYan
# @Date  : 2019/3/8
# @Desc  : ansible 封装
# flake8: noqa
import os
import sys

from collections import namedtuple
from ansible.parsing.dataloader import DataLoader
from ansible.vars.manager import VariableManager
from ansible.inventory.manager import InventoryManager
from ansible.playbook.play import Play
from ansible.executor.task_queue_manager import TaskQueueManager
from ansible.plugins.callback import CallbackBase
from ansible import constants as C

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../'))
sys.path.append(os.path.join(os.environ['SENSORS_PLATFORM_HOME'], 'pycommon'))
from common.pyutils.sensors_recommend_config_utils import get_sr_home_path
from hyperion_client.deploy_topo import DeployTopo
from hyperion_client.node_info import NodeInfo

SENSORS_RECOMMENDER_HOME = get_sr_home_path()
INVENTORY_PATH = os.path.join(SENSORS_RECOMMENDER_HOME, 'monitor', 'hosts')
C.HOST_KEY_CHECKING = False
TIMEOUT = 30

DEFAULT_USER = 'sa_cluster'


def generate_inventory_file(inventory_path):
    with open(inventory_path, 'w') as f:
        f.writelines('[web_node]\n')
        sr_online_node_list = DeployTopo().get_host_list_by_role_group_name('sr_online')
        for web_node in sr_online_node_list:
            line = web_node
            ssh_port = NodeInfo().get_node_ssh_port(web_node)
            line += ' ansible_port={port}'.format(port=ssh_port)
            line += '\n'
            f.writelines(line)

        f.writelines('[data_node]\n')
        for node in DeployTopo().get_all_host_list():
            if node in sr_online_node_list:
                continue
            line = node
            ssh_port = NodeInfo().get_node_ssh_port(web_node)
            line += ' ansible_port={port}'.format(port=ssh_port)
            line += '\n'
            f.writelines(line)


class AnsibleHelper(object):
    """
    帮你初始化，操作ansible
    """

    class ResultCallback(CallbackBase):
        """A sample callback plugin used for performing an action as results come in

        If you want to collect all results into a single object for processing at
        the end of the execution, look into utilizing the ``json`` callback plugin
        or writing your own custom callback plugin
        """

        def __init__(self):
            self.result_dict = {}
            # init log handler for every project

        def v2_runner_on_ok(self, result, **kwargs):
            """Print a json representation of the result

            This method could store the result in an instance attribute for retrieval later
            """
            host = result._host
            self.result_dict[host.name] = result._result
            # 打成功日志

        def v2_runner_on_failed(self, result, **kwargs):
            host = result._host
            self.result_dict[host.name] = result._result
            # 打失败日志

        def v2_runner_on_unreachable(self, result, **kwargs):
            host = result._host
            self.result_dict[host.name] = 'An error occured for host ' + str(
                result._host) + ' with the following message:\n\n' + str(
                result._result)

        def clear(self):
            self.result_dict = {}

    def __init__(self, inventory=INVENTORY_PATH, forks=10):
        generate_inventory_file(inventory)
        self.inventory = inventory
        Options = namedtuple('Options',
                             ['connection', 'module_path', 'forks', 'become', 'become_method', 'become_user', 'check',
                              'diff', 'timeout',
                              ])
        # initialize needed objects
        self.loader = DataLoader()
        self.options = Options(connection='ssh',
                               module_path='{sr_home_path}/python/lib/python3.6/site-packages/ansible/modules'.format(
                                   sr_home_path=SENSORS_RECOMMENDER_HOME),
                               forks=forks, become=None,
                               become_method='sudo', become_user=DEFAULT_USER, check=False,
                               diff=False, timeout=TIMEOUT)
        self.passwords = dict(vault_pass='secret')
        self.result_list = []
        # Instantiate our ResultCallback for handling results as they come in
        self.results_callback = self.ResultCallback()
        # self.variable =

    def run(self, play_source):
        # play_source = dict(
        #     name="Ansible Play",
        #     hosts='localhost',
        #     gather_facts='no',
        #     tasks=[
        #         dict(action=dict(module='shell', args='ls'), register='shell_out'),
        #         dict(action=dict(module='debug', args=dict(msg='{{shell_out.stdout}}')))
        #     ]
        # )
        inventory = InventoryManager(loader=self.loader, sources=[self.inventory])
        variable = VariableManager(loader=self.loader, inventory=inventory)
        play = Play().load(play_source, variable_manager=variable, loader=self.loader)

        # actually run it
        tqm = None
        try:
            tqm = TaskQueueManager(
                inventory=inventory,
                variable_manager=variable,
                loader=self.loader,
                options=self.options,
                passwords=self.passwords,
                stdout_callback=self.results_callback,
                # Use our custom callback instead of the ``default`` callback plugin
            )
            result = tqm.run(play)
            return result, self.results_callback.result_dict
        finally:
            if tqm is not None:
                tqm.cleanup()
            # todo: clean tmp log

    def generate_playsource(self, host, user, module, args_dict):
        playsource = dict(name='sr_admin',
                          hosts=host,
                          gather_facts='no',
                          tasks=[
                              dict(action=dict(module=module,
                                               args=args_dict,
                                               ) if args_dict else dict(module=module)
                                   )
                          ]
                          )
        if user:
            playsource['become'] = 'yes'
            playsource['become_user'] = user
        return playsource

    def command(self, host, user=None, **kwargs):
        """
        在远程机器执行命令，非shell，不指定用户的话会读sa_cluster的.bashrc
        :param host: 执行的机器 例子： [all];[data_node];[web_node];[rec01,rec02,data01]
        :param user: 执行机上的用户 例: root;sa_cluster
        :param kwargs:

        cmd: required=True, help='执行的命令'
        chdir: help='运行前跳转目录'
        creates: help='创建一个文件，如果文件存在，不会生效'
        removes: help='删除文件'

        :return: state: 命令执行状态（0 正常 1 异常）
                 message: ansible返回的信息
        """
        playsource = self.generate_playsource(host=host, user=user, module='command', args_dict=kwargs)
        state, message = self.run(play_source=playsource)
        self.results_callback.clear()
        return state, message

    def copy(self, host, user=None, **kwargs):
        """
        copy local file to remote hosts
        :param host: 执行的机器 例子： [all];[data_node];[web_node];[rec01,rec02,data01]
        :param user: 执行机上的用户 例: root;sa_cluster
        :param kwargs:

        dest:  required=True, help='目标路径'
        backup: choices=['yes', 'no'], help='如果远端有同名文件是否备份，默认no'
        force: choices=['yes', 'no'], help='默认yes, no的时候只会在远程没有这个文件的时候执行'
        owner: help='文件属性，默认为执行的用户'
        group: help='文件属性，默认为执行的用户组'
        src: help='src 和 content 二选一： src为文件或文件夹路径，content为文件内容'
        content: help='src 和 content 二选一： src为文件或文件夹路径，content为文件内容'
        mode: help='文件属性 0755 或 u=rw,g=r,o=r都可以'

        :return: state: 命令执行状态（0 正常 1 异常）
                 message: ansible返回的信息
        """
        playsource = self.generate_playsource(host=host, user=user, module='copy', args_dict=kwargs)
        state, message = self.run(play_source=playsource)
        self.results_callback.clear()
        return state, message

    def fetch(self, host, user=None, **kwargs):
        """
        fetch remote hosts file to local
        :param host: 执行的机器 例子： [all];[data_node];[web_node];[rec01,rec02,data01]
        :param user: 执行机上的用户 例: root;sa_cluster
        :param kwargs:

        parser_fetch.add_argument('-d', '--dest', required=True, help='拉到的本地路径')
        parser_fetch.add_argument('-s', '--src', required=True, help='远程文件源地址')

        :return: state: 命令执行状态（0 正常 1 异常）
                 message: ansible返回的信息
        """
        playsource = self.generate_playsource(host=host, user=user, module='fetch', args_dict=kwargs)
        state, message = self.run(play_source=playsource)
        self.results_callback.clear()
        return state, message

    def shell(self, host, user=None, **kwargs):
        """
        远程用shell执行命令 不会读bashrc
        :param host: 执行的机器 例子： [all];[data_node];[web_node];[rec01,rec02,data01]
        :param user: 执行机上的用户 例: root;sa_cluster
        :param kwargs:

        cmd: required=True, help='执行的命令'
        chdir:  help='运行前跳转目录'
        creates: help='文件名，如果文件存在，不会生效'
        removes: help='文件名，如果不存不会生效'
        executable: help='选择运行的shell版本 例/bin/bash'

        :return: state: 命令执行状态（0 正常 1 异常）
                 message: ansible返回的信息
        """
        playsource = self.generate_playsource(host=host, user=user, module='shell', args_dict=kwargs)
        state, message = self.run(play_source=playsource)
        self.results_callback.clear()
        return state, message

    def ping(self, host, user=None, **kwargs):
        playsource = self.generate_playsource(host=host, user=user, module='ping', args_dict=kwargs)
        state, message = self.run(play_source=playsource)
        self.results_callback.clear()
        return state, message

    def service(self, host, user=None, project=None, **kwargs):
        """
        查看sr组件的状态，启停
        :param host: 执行的机器 例子： [all];[data_node];[web_node];[rec01,rec02,data01]
        :param user: 执行机上的用户 例: root;sa_cluster
        :param project: 项目名
        :param kwargs:
        name: choices=['sr_nginx', 'biz_redis', 'cache_redis', 'model_service', 'web_service',
                                         'meta_data_service', 'azkaban',
                                         'sr_supervisor'], required=True, help='组件名称'
        state: choices=['started', 'stopped', 'reloaded', 'restarted', 'status'],
                                required=True, help='操作的类型'

        :return: state: 命令执行状态（0 正常 1 异常）
                 message: ansible返回的信息
        """
        PLAY_SOURCE = {
            'web_service': {
                'started': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf start {project_name}_recommend_web_service:",
                'stopped': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf stop {project_name}_recommend_web_service:",
                'restarted': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c /{SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf restart {project_name}_recommend_web_service:",
                'reloaded': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf reload",
                'status': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf status {project_name}_recommend_web_service:",
            },
            'model_service': {
                'started': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf start {project_name}_model_service:",
                'stopped': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf status {project_name}_model_service:",
                'restarted': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c /{SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf restart {project_name}_model_service:",
                'reloaded': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf reload",
                'status': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf status {project_name}_model_service:",
            },
            'biz_redis': {
                'started': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf start {project_name}_biz_redis_{biz_port}",
                'stopped': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf stop {project_name}_biz_redis_{biz_port}",
                'restarted': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf restart {project_name}_biz_redis_{biz_port}",
                'reloaded': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf reload",
                'status': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf status {project_name}_biz_redis_{biz_port}",
            },
            'cache_redis': {
                'started': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf start {project_name}_cache_redis_{cache_port}",
                'stopped': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf stop {project_name}_cache_redis_{cache_port}",
                'restarted': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf restart {project_name}_cache_redis_{cache_port}",
                'reloaded': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf reload",
                'status': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf status {project_name}_cache_redis_{cache_port}",
            },
            'grafana': {
                'started': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf start grafana",
                'stopped': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf stop grafana",
                'restarted': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf restart grafana",
                'reloaded': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf reload",
                'status': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf status grafana",
            },
            'influxdb': {
                'started': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf start influxdb",
                'stopped': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf stop influxdb",
                'restarted': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf restart influxdb",
                'reloaded': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf reload",
                'status': "{SENSORS_RECOMMENDER_HOME}/supervisor/bin/supervisorctl -c {SENSORS_RECOMMENDER_HOME}/conf/supervisor.d/supervisord.conf status influxdb",
            },

        }
        from moniter_conf import projects_conf, project_list, host_mapping
        # 需要适应其它方式启动的服务
        redis_biz_dict = dict((x, projects_conf[x]['redis_server_configs']['biz']) for x in project_list)
        redis_cache_dict = dict(
            (x, projects_conf[x]['redis_server_configs']['cache']) for x in project_list)

        SUPERVISOR_SERVICE = ['web_service', 'model_service', 'biz_redis', 'cache_redis', 'grafana', 'influxdb']
        SYSTEM_SERVICE = ['sr_nginx', 'sr_supervisor']

        DEFAULT_REDIS_BIZ_PORT = 6579
        DEFAULT_REDIS_CACHE_PORT = 6679

        if kwargs['name'] in SYSTEM_SERVICE:
            # systemctl控制的服务
            if kwargs['state'] == 'status':
                kwargs['state'] = 'started'
            playsource = dict(name='fetchs a file from remote nodes',
                              hosts=host,
                              gather_facts='no',
                              tasks=[
                                  dict(action=dict(module='service',
                                                   args=kwargs,
                                                   )
                                       )
                              ]
                              )
        elif kwargs['name'] in SUPERVISOR_SERVICE:
            biz_port = DEFAULT_REDIS_BIZ_PORT
            cache_port = DEFAULT_REDIS_CACHE_PORT
            if 'redis' in kwargs['name']:
                for i in redis_biz_dict[project]:
                    if isinstance(host, str):
                        if i[0] == host_mapping[host]:
                            biz_port = i[1]
                    if isinstance(host, list):
                        if i[0] == host_mapping[host[0]]:
                            biz_port = i[1]
                for i in redis_cache_dict[project]:
                    if isinstance(host, str):
                        if i[0] == host_mapping[host]:
                            cache_port = i[1]
                    if isinstance(host, list):
                        if i[0] == host_mapping[host[0]]:
                            cache_port = i[1]
            playsource = self.generate_playsource(host=host, user=user, module='command',
                                                  args_dict=PLAY_SOURCE[kwargs['name']][kwargs['state']].format(
                                                      SENSORS_RECOMMENDER_HOME=SENSORS_RECOMMENDER_HOME,
                                                      project_name=project, biz_port=biz_port, cache_port=cache_port))
        state, message = self.run(play_source=playsource)
        self.results_callback.clear()
        return state, message

# ansible_a = AnsibleHelper()
# ansible_a.run(
#     play_source=dict(
#     name="Ansible Play",
#     hosts='sb701',
#     gather_facts='no',
#     tasks=[
#         dict(action=dict(module='shell', args='ls'), register='shell_out'),
#         dict(action=dict(module='ping'))
#     ]
# ))
