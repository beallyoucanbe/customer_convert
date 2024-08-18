#!/home/sa_cluster/sr/python/bin/python3
"""Reids 工具箱.

功能：
    * 自动连接 master redis server
    * 扫描 biz redis 里的所有 key 的 format
    * 删除符合 format 的 key
    * 列出 exp_config.py 使用的 redis key
    * 可以获取被压缩的 key 的 value
"""
import argparse
import os
import pprint
import re
import sys
import redis

from importlib.util import module_from_spec, spec_from_file_location
from itertools import islice

sys.path.append("/home/sa_cluster/sr/")
from common.pyutils import zip_data
from common.data_flow.mario.conf_parser import ConfParser

__version__ = "0.0.2"


def read_exp_config(company_path, project_name, redis_client):
    print("List keys in exp_config...")
    exp_config_path = os.path.join(
        company_path, project_name, "web_service/data/exp_config.py"
    )
    if not os.path.isfile(exp_config_path):
        print("exp_config.py is not exists, skip listing keys.")
        return

    spec = spec_from_file_location("exp_config", exp_config_path)
    exp_config = module_from_spec(spec)
    spec.loader.exec_module(exp_config)

    key_formats = list()
    for k in exp_config.__dir__():
        if not k.startswith("__") and k.startswith("get"):
            v = getattr(exp_config, k)
            if isinstance(v, dict):
                key_formats.append((k, v))

    pprint.pprint(key_formats)

    k_set = set()
    for g in key_formats:
        print(g[0], ":")
        for k, v in g[1].items():
            print(f"  {v}:")
            v_match = re.sub(r"{.*?}", "*", v)

            # 没有挖空的 key 其取值为其自身
            if v_match == v:
                continue

            # 出现过的 key 不进行重复展示
            if v_match in k_set:
                print("...")
                continue
            else:
                k_set.add(v_match)

            it = redis_client.scan_iter(match=v_match, count=4)
            for i, item in enumerate(it):
                print(f"  ({i}): ", item)
                if i >= 3:
                    break


def list_all_key(client):
    print("List all key format...")
    key_set = set()
    it = client.scan_iter(count=1000)
    for key in it:
        kf = key.split(b":")[0]
        if kf not in key_set:
            key_set.add(kf)
            print(key.decode())


def del_keys(key_format, redis_client):
    print("Delete keys...")
    del_num = 0
    it = redis_client.scan_iter(match=key_format, count=1000)
    while True:
        keys = [i for i in islice(it, 500)]
        if keys:
            redis_client.delete(*keys)
            del_num += len(keys)
            print(f"{del_num}...", end="\r", flush=True)
        else:
            print("\nDone!")
            break


def main():
    parser = argparse.ArgumentParser(description="Model Service Benchmark Tool")
    parser.add_argument("-p", "--project", required=True, help="Project's name.")
    parser.add_argument(
        "-l",
        "--list",
        action="store_true",
        help="List all key format in exp_config.py and scan some keys.",
    )
    parser.add_argument("-d", "--delete", help="Delete all keys match the format.")
    parser.add_argument(
        "-x", "--compress", action="store_true", help="Value compress(zlib) flag."
    )
    args = parser.parse_args()

    conf = ConfParser()

    if args.project not in conf.sr_conf["project_names"]:
        print("Project name is not exists, quit...")
        exit(1)

    # biz_master ('10.42.22.215', 6501) -> {'host': '10.42.22.215', 'port': 6501, 'password': 'MhxzKhl2015'}
    biz_master = conf.projects_conf[args.project]["redis_server_configs"]["biz_master"]
    if not isinstance(biz_master, dict):
        biz_master = {'host': biz_master[0], 'port': biz_master[1], 'password': None}
    client = redis.Redis(host=biz_master['host'], port=biz_master['port'], password=biz_master.get('password'))

    if not client.ping():
        print("Redis server is down!")
        print("Exit...")
        exit(1)

    if args.list:
        cmd = input("use exp_config.py ? (yes or no): ")
        if cmd.lower()[0] == "y":
            read_exp_config(conf.sr_conf["company_path"], args.project, client)
        else:
            list_all_key(client)

    if args.delete:
        del_keys(args.delete, client)

    while True:
        key = input("Get Key: ")
        v = client.get(key)
        v = zip_data.decrypt_data(v)
        print(v.decode())


if __name__ == "__main__":
    main()
