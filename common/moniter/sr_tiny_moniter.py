#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : sr_tiny_moniter.py.py
# @Author: DingYan
# @Date  : 2019/3/8
# @Desc  : sr_运维工具
# fuction：: 多机服务的启停，多机文件的拷贝分发，多机
# flake8: noqa
import os
import sys
import json
import argparse

from importlib.machinery import SourceFileLoader

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../'))
from common.pyutils import sensors_recommend_config_utils
from common.moniter.moniter_conf import sr_recommend_home
from common.moniter.ansible_helper import AnsibleHelper

sr_conf = sensors_recommend_config_utils.get_sr_conf()


def default_print_function(x):
    print(x, file=sys.stderr)


def extract_dict_by_list(args, list):
    args_dict = {}
    for arg in list:
        if getattr(args, arg):
            args_dict[arg] = getattr(args, arg)
    args.hosts = args.hosts.strip().split(',')
    return args_dict


def assert_msg(state, msg):
    if state != 0:
        raise Exception("failed! message=%s" % (msg))


def generate_output(state, message, verbose, print_fun, throw_except=True):
    if verbose:
        print_fun(json.dumps(message, indent=4))
    else:
        print_fun(json.dumps({host: {"stdout_lines": host_message.get("stdout_lines", ''),
                                     "stderr_lines": host_message.get("stderr_lines", '')} if host_message.get(
            "stderr_lines", '') else {
            "stdout_lines": host_message.get("stdout_lines", '')} for host, host_message in message.items()}, indent=4))
    if throw_except:
        assert_msg(state, json.dumps(message, indent=4))


def ping(args, print_fun):
    def generate_output(state, message, verbose, print_fun, throw_except=True):
        if verbose:
            print_fun(json.dumps(message, indent=4))
        else:
            print_fun(
                json.dumps({host: {'ping': host_message['ping']} for host, host_message in message.items()}, indent=4))
        if throw_except:
            assert_msg(state, json.dumps(message, indent=4))

    ansible_client = AnsibleHelper(inventory=args.inventory, forks=int(args.forks))
    state, message = ansible_client.ping(host=args.hosts, user=args.user)
    print(state, message)
    generate_output(state, message, args.verbose, print_fun)


def copy(args, print_fun):
    ansible_client = AnsibleHelper(inventory=args.inventory, forks=int(args.forks))
    args_list = ['dest', 'backup', 'force', 'owner', 'group', 'src', 'content', 'mode']
    args_dict = extract_dict_by_list(args, args_list)
    state, message = ansible_client.copy(host=args.hosts, user=args.user, **args_dict)

    generate_output(state, message, args.verbose, print_fun)


def fetch(args, print_fun):
    ansible_client = AnsibleHelper(inventory=args.inventory, forks=int(args.forks))
    args_list = ['dest', 'src']
    args_dict = extract_dict_by_list(args, args_list)
    state, message = ansible_client.fetch(host=args.hosts, user=args.user, **args_dict)
    generate_output(state, message, args.verbose, print_fun)


def command(args, print_fun):
    ansible_client = AnsibleHelper(inventory=args.inventory, forks=int(args.forks))
    args_list = ['cmd', 'chdir', 'creates', 'removes']
    args_dict = extract_dict_by_list(args, args_list)
    state, message = ansible_client.command(host=args.hosts, user=args.user, **args_dict)
    generate_output(state, message, args.verbose, print_fun)


def shell(args, print_fun):
    ansible_client = AnsibleHelper(inventory=args.inventory, forks=int(args.forks))
    args_list = ['cmd', 'chdir', 'creates', 'removes', 'executable']
    args_dict = extract_dict_by_list(args, args_list)
    state, message = ansible_client.shell(host=args.hosts, user=args.user, **args_dict)
    generate_output(state, message, args.verbose, print_fun)


# def replace(args, print_fun):
#     ansible_client = AnsibleHelper(inventory=args.inventory, forks=int(args.forks))
#     args_list = ['path', 'regexp', 'replace', 'backup', 'before', 'after']
#     args_dict = extract_dict_by_list(args, args_list)
#     state, message = ansible_client.replace(host=args.hosts, user=args.user, **args_dict)
#     generate_output(state, message, args.verbose, print_fun)


def service(args, print_fun):
    ansible_client = AnsibleHelper(inventory=args.inventory, forks=int(args.forks))
    args_list = ['name', 'project', 'state']
    args_dict = extract_dict_by_list(args, args_list)
    state, message = ansible_client.service(host=args.hosts, user=args.user, **args_dict)
    generate_output(state, message, args.verbose, print_fun)


def list_hosts(args, print_fun):
    host_dict = {}
    with open(args.inventory, 'r') as f:
        group_name = None
        host_list = []
        for line in f.read().strip().split('\n'):
            if '[' in line:
                if group_name is not None:
                    host_dict[group_name] = host_list
                group_name = line
                host_list = []
            else:
                host_list.append(line)
        host_dict[group_name] = host_list
    print_fun(json.dumps(host_dict, indent=4))


def list_modules(args, print_fun):
    # todo 现在都存zk里面 改成从zk读
    sr_conf = SourceFileLoader('sr_conf', f'{sr_recommend_home}/conf/sensors_recommend_conf.py').load_module()

    project_name = args.project
    host_dict = sr_conf.sensors_recommend_exec_server_configs.get('data_node', {})
    host_dict.update(sr_conf.sensors_recommend_exec_server_configs.get('web_node', {}))
    message_dict = {}
    project_conf = SourceFileLoader('sr_conf_object',
                                    f'{sr_recommend_home}/conf/' + project_name + '/sensors_recommend_project_conf.py').load_module()
    if args.hosts == 'all':
        host_list = list(host_dict.keys())
    else:
        host_list = args.hosts.split(',')
    for host in host_list:
        _dict = {}
        # nginx
        try:
            if host in list(sr_conf.nginx_conf.values())[0]:
                for i in project_conf.sensors_recommend_nginx_addresses:
                    if i[0] == host_dict[host]['host']:
                        _dict['nginx'] = {'port': i[1]}
        except Exception:
            pass

        # redis
        try:
            # sensors_recommend_redis_server_configs = {
            #     "biz": [('10.42.22.215', 6501)],
            #     "cache": [('10.42.22.215', 6601)],
            #     "biz_master": ('10.42.22.215', 6501),
            # }
            # biz, cache [('10.42.22.215', 6501)] -> [{'host': '10.42.22.215', 'port': 6501, 'password': 'MhxzKhl2015'}]
            # biz_master ('10.42.22.215', 6501) -> {'host': '10.42.22.215', 'port': 6501, 'password': 'MhxzKhl2015'}
            for biz_redis in project_conf.sensors_recommend_redis_server_configs['biz']:
                if not isinstance(biz_redis, dict):
                    biz_redis = {'host': biz_redis[0], 'port': biz_redis[1], 'password': None}
                if biz_redis['host'] == host_dict[host]['host']:
                    _dict['biz_redis'] = biz_redis
            for cache_redis in project_conf.sensors_recommend_redis_server_configs['cache']:
                if not isinstance(cache_redis, dict):
                    cache_redis = {'host': cache_redis[0], 'port': cache_redis[1], 'password': None}
                if cache_redis['host'] == host_dict[host]['host']:
                    _dict['cache_redis'] = cache_redis
            biz_master = project_conf.sensors_recommend_redis_server_configs['biz_master']
            if not isinstance(biz_master, dict):
                biz_master = {'host': biz_master[0], 'port': biz_master[1], 'password': None}
            if host_dict[host]['host'] == biz_master['host']:
                _dict['biz_master'] = biz_master
        except Exception:
            pass

        # web_service 默认webnode都有

        try:
            if host in sr_conf.sensors_recommend_exec_server_configs['web_node']:
                _dict['web_service'] = {'port': 'read nginx config or supervisor config'}
        except Exception:
            pass

        # model_service

        try:
            for i in project_conf.sensors_recommend_model_service_addresses:
                for j in project_conf.sensors_recommend_model_service_addresses[i]:
                    if j[0] == host_dict[host]['host']:
                        if 'model_service' in _dict:
                            _dict['model_service'].update({i: j[1]})
                        else:
                            _dict['model_service'] = {i: j[1]}
        except Exception:
            pass

        # azkaban

        try:
            if sr_conf.azkaban_conf['host'] == host:
                _dict['azkaban'] = {'executor_port': sr_conf.azkaban_conf['executor_port'],
                                    'web_port': sr_conf.azkaban_conf['web_port']}
        except Exception:
            pass
        # influxdb
        try:
            if sr_conf.influxdb_conf['host'] == host:
                _dict['influxdb'] = {'port': sr_conf.influxdb_conf['port']}
        except Exception:
            pass

        # grafana

        try:
            if sr_conf.grafana_conf['host'] == host:
                _dict['grafana'] = {'port': sr_conf.grafana_conf['port']}
        except Exception:
            pass

        message_dict[host] = _dict

    print_fun(json.dumps(message_dict, indent=4))


def init_argparser():
    parser = argparse.ArgumentParser(description='SR admin')
    subparsers = parser.add_subparsers(help='sub-command help', prog='sr_admin modules')
    parser.add_argument('-H', '--hosts', required=True, help='连接的机器名')
    parser.add_argument('-u', '--user', help='运行账户')
    parser.add_argument('-i', '--inventory', default='{sr_home_path}/moniter/hosts'.format(
        sr_home_path=sensors_recommend_config_utils.get_sr_home_path()),
                        help='要加载的hosts文件')
    parser.add_argument('-f', '--forks', default=10, help='执行命令时的线程数')
    parser.add_argument('-v', '--verbose', action='store_true', default=False, help='显示全部信息')

    # parser.set_defaults(func=invoke)
    # ping
    parser_ping = subparsers.add_parser('ping', help='ping remote hosts')
    parser_ping.set_defaults(func=ping)
    # copy
    parser_copy = subparsers.add_parser('copy', help='copy local file to remote hosts')
    parser_copy.add_argument('-d', '--dest', required=True, help='目标路径')
    parser_copy.add_argument('-b', '--backup', choices=['yes', 'no'], help='如果远端有同名文件是否备份，默认no')
    parser_copy.add_argument('-f', '--force', choices=['yes', 'no'], help='默认yes, no的时候只会在远程没有这个文件的时候执行')
    parser_copy.add_argument('-o', '--owner', help='文件属性，默认为执行的用户')
    parser_copy.add_argument('-g', '--group', help='文件属性，默认为执行的用户组')
    group_copy = parser_copy.add_mutually_exclusive_group()
    group_copy.add_argument('-s', '--src', help='src 和 content 二选一： src为文件或文件夹路径，content为文件内容')
    group_copy.add_argument('-c', '--content', help='src 和 content 二选一： src为文件或文件夹路径，content为文件内容')
    parser_copy.add_argument('-m', '--mode', help='文件属性 0755 或 u=rw,g=r,o=r都可以')
    parser_copy.set_defaults(func=copy)
    # fetch
    parser_fetch = subparsers.add_parser('fetch', help='fetch remote hosts file to local')
    parser_fetch.add_argument('-d', '--dest', required=True, help='拉到的本地路径')
    parser_fetch.add_argument('-s', '--src', required=True, help='远程文件源地址')
    parser_fetch.set_defaults(func=fetch)

    # command
    parser_command = subparsers.add_parser('command', help='在远程机器执行命令，非shell，不指定用户的话会读sa_cluster的.bashrc')
    parser_command.add_argument('-c', '--cmd', required=True, help='执行的命令')
    parser_command.add_argument('--chdir', help='运行前跳转目录')
    parser_command.add_argument('--creates', help='创建一个文件，如果文件存在，不会生效')
    parser_command.add_argument('--removes', help='删除文件')
    parser_command.set_defaults(func=command)

    # shell
    parser_shell = subparsers.add_parser('shell', help='远程用shell执行命令')
    parser_shell.add_argument('-c', '--cmd', required=True, help='执行的命令')
    parser_shell.add_argument('--chdir', help='运行前跳转目录')
    parser_shell.add_argument('--creates', help='文件名，如果文件存在，不会生效')
    parser_shell.add_argument('--removes', help='文件名，如果不存不会生效')
    parser_shell.add_argument('--executable', help='选择运行的shell版本 例/bin/bash')
    parser_shell.set_defaults(func=shell)

    # # replace
    # parser_replace = subparsers.add_parser('replace', help='远程用shell执行命令')
    # parser_replace.add_argument('-p', '--path', required=True, help='正则替换的文件路径')
    # parser_replace.add_argument('-r', '--regexp', required=True, help='正则匹配规则')
    # parser_replace.add_argument('--replace', required=True, help='替换的内容')
    # parser_replace.add_argument('-b', '--backup', choices=['yes', 'no'], help='备份')
    # parser_replace.add_argument('--before', help='打出替换行之前的样子')
    # parser_replace.add_argument('--after', help='同上')
    # parser_replace.set_defaults(func=replace)

    # service
    parser_service = subparsers.add_parser('service', help='操作的sr组件')
    parser_service.add_argument('-n', '--name',
                                choices=['sr_nginx', 'biz_redis', 'cache_redis', 'model_service', 'web_service',
                                         'meta_data_service', 'azkaban',
                                         'sr_supervisor'], required=True, help='组件名称')
    parser_service.add_argument('-p', '--project', required=True, help='项目名称')
    parser_service.add_argument('-a', '--state', choices=['started', 'stopped', 'reloaded', 'restarted', 'status'],
                                required=True, help='操作的类型')
    parser_service.set_defaults(func=service)

    # list_hosts
    parser_list_hosts = subparsers.add_parser('list_hosts', help='显示host group信息')
    parser_list_hosts.set_defaults(func=list_hosts)

    # list_modules
    parser_list_alarm = subparsers.add_parser('list_modules', help='显示指定机器上的组件信息')
    parser_list_alarm.add_argument('-p', '--project', required=True, help='项目名')
    parser_list_alarm.set_defaults(func=list_modules)

    return parser


def main():
    parser = init_argparser()
    args = parser.parse_args()
    args.func(args, print_fun=default_print_function)


if __name__ == '__main__':
    main()
