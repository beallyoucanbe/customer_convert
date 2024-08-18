#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : moniter_surface.py.py
# @Author: DingYan
# @Date  : 2019/6/5
# @Desc  :
import os
import sys
import json
import argparse

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../'))

from common.moniter.moniter_conf import sr_conf, projects_conf
from common.moniter.tiny_moniter import TinyMoniter


def invoke_moniter(args):
    tm = TinyMoniter(sr_conf, projects_conf)
    if args.monitor:
        output_dict = tm.do_moniter(module=args.module, submodule=args.submodule, prop=args.prop, hosts=[args.host], project_name=args.project_name)
        print(json.dumps({'value': output_dict[args.host][0]}, indent=4))
    else:
        output_dict = tm.do_moniter(module=args.module, submodule=args.submodule, prop=args.prop, hosts=[args.host], project_name=args.project_name)
        if args.verbose:
            print(output_dict[args.host])
        else:
            print(output_dict[args.host][0])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='SR Moniter')
    parser.add_argument('-H', '--host', required=True, help='机器名 只能单台机器')
    parser.add_argument('-n', '--project_name', required=True, help='项目名称')
    parser.add_argument('-m', '--module', help='模块')
    parser.add_argument('-s', '--submodule', required=True, help='子模块')
    parser.add_argument('-p', '--prop', required=True, help='子模块属性')
    parser.add_argument('-v', '--verbose', action='store_true', default=False, help='显示全部信息')
    parser.add_argument('--monitor', action='store_true', default=False, help='要发给sa监控项才加此参数')
    parser.set_defaults(func=invoke_moniter)
    args = parser.parse_args()
    args.func(args)
