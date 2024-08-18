import argparse
import atexit
import datetime
import json
import os
import readline
import subprocess
import tempfile
import uuid
from itertools import chain
from pprint import pprint
from urllib.parse import urljoin

import requests
import yaml

__VERSION__ = "0.0.3"

SENSORS_RECOMMEND_HOME = "/home/sa_cluster/sr"

log_id = f"PROBIUS-{uuid.uuid4()}"
exp_id = "sensors_rec"
histfile = os.path.join(os.path.expanduser("~"), ".probius_history")

try:
    readline.read_history_file(histfile)
    h_len = readline.get_current_history_length()
except FileNotFoundError:
    open(histfile, "wb").close()
    h_len = 0


def save(prev_h_len, histfile):
    new_h_len = readline.get_current_history_length()
    readline.set_history_length(1000)
    readline.append_history_file(new_h_len - prev_h_len, histfile)


atexit.register(save, h_len, histfile)


def _input(prompt):
    i = input(prompt)
    if i.isdigit():
        return int(i)
    else:
        return -1


class Probius(object):
    def __init__(self, conf_path=None, quick=False, _input=False, key_option=None, key_val=None, ref_id=1):
        sr_conf_path = (
            conf_path
            if conf_path
            else os.path.join(
                os.environ.get("SENSORS_RECOMMEND_HOME", SENSORS_RECOMMEND_HOME),
                "conf",
                "probius_conf.yaml",
            )
        )
        with open(sr_conf_path, "rb") as f:
            self.conf = yaml.load(f, Loader=yaml.SafeLoader)

        self.quick_mode = quick
        self.input_mode = _input
        self.key_option = key_option or []
        self.key_option = {
            k: int(v) for k, v in (arg.split("=") for arg in self.key_option)
        }
        self.key_val = key_val or []
        self.key_val = {
            k: v for k, v in (arg.split("=") for arg in self.key_val)
        }
        self.ref_id = ref_id

    def _input_arg(self, args_name):
        arg = None
        conf_arg = self.conf.get(args_name)
        if conf_arg is not None:
            if not isinstance(conf_arg, list):
                conf_arg = [conf_arg]

            if args_name in self.key_option:
                option = self.key_option[args_name]
            elif self.quick_mode:
                option = 0
            else:
                if conf_arg:
                    if len(conf_arg) == 1:
                        print(f"'{args_name}' 自动选择：{conf_arg[0]}")
                        option = 0
                    else:
                        print(f"'{args_name}'的可选参数（空则进入手动输入）：")
                        for i, a in enumerate(conf_arg):
                            print(f"{i}. {a}")
                        option = _input("-> ")
                else:
                    option = -1

            if option < 0:
                arg = input(f"手动输入 '{args_name}' -> ")
            else:
                arg = conf_arg[option]
        else:
            arg = input(f"手动输入 '{args_name}' -> ")
        return arg

    def _prepare_args(self):
        body = dict()

        for arg_name in self.conf.keys():
            if arg_name not in ("compat_args", "sql"):
                body[arg_name] = self._input_arg(arg_name)

        # 将指定参数值覆盖进去
        if self.key_val:
            body.update(self.key_val)

        # 地址参数
        nginx_adress = body.pop("nginx")
        api = body.pop("api")
        url = urljoin(nginx_adress, api)

        # 替换参数
        compat_args = self.conf.get("compat_args", {})
        for k, v in compat_args.items():
            if k in body:
                body[v] = body.pop(k)

        # 将参数值全部转成 str
        for k in list(body.keys()):
            if not isinstance(body[k], str):
                body[k] = str(body[k])

        pprint(f"url: {url}")
        pprint("body: (debug 参数会自动添加)")
        pprint(body)

        # 额外参数
        while True:
            other_arg = input("额外参数名（空则终止），可填 url -> ")
            if other_arg:
                v = input("参数值 -> ")
                if other_arg == "url":
                    url = v
                else:
                    body[other_arg] = v
            else:
                break

        return url, body

    def run(self, output_dir=None, key_val=None):
        if self.input_mode:
            url = input("URL: ")
            body = input("JSON: ")
            body = json.loads(body)
        else:
            url, body = self._prepare_args()

        # debug 参数
        body["debug"] = "1"

        r = requests.post(url, json=body)
        if r.status_code not in (200, 299):
            print(f"status code {r.status_code}, exit...")
            return

        rets = r.json()
        debug_infos = rets.pop("debug_infos")

        item_infos = dict()
        if "sql" in self.conf:
            print("附上 item 其它信息...")
            items = set()
            for df_func in debug_infos.get("data_fetch", []):
                for i in chain(df_func.get("inputs", []), df_func.get("results", [])):
                    items.add((i["item_id"], i["item_type"]))

            for i in chain(debug_infos.get("rank", []), debug_infos.get("rerank", [])):
                items.add((i["item_id"], i["item_type"]))

            tmp_sql_file = tempfile.mktemp(suffix="sql")
            data_file = tempfile.mktemp(suffix="data")
            with open(tmp_sql_file, "w") as f:
                f.write(
                    self.conf["sql"].format(",".join(f"'{x},{y}'" for x, y in items))
                )
            ret = subprocess.call(
                f"sradmin infinity -m query -p {self.ref_id} -f {tmp_sql_file} -o {data_file}",
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                shell=True,
            )
            if ret == 0:
                with open(data_file, "r") as f:
                    for line in f:
                        item_id, item_type, *other_info = line.split("\001")
                        item_infos[(item_id, item_type)] = other_info
            else:
                print("WARNING: SQL 执行错误，请检查配置是否正确，不会附上 item 信息.")

        if item_infos:
            for df_func in debug_infos.get("data_fetch", []):
                for i in chain(df_func.get("inputs", []), df_func.get("results", [])):
                    i["other_infos"] = item_infos.get((i["item_id"], i["item_type"]))

            for i in chain(debug_infos.get("rank", []), debug_infos.get("rerank", [])):
                i["other_infos"] = item_infos.get((i["item_id"], i["item_type"]))

            for i in rets.get("data", []):
                i["other_infos"] = item_infos.get((i["item_id"], i["item_type"]))

        output_dir = output_dir if output_dir else "."
        os.makedirs(output_dir, exist_ok=True)
        filename = os.path.join(
            output_dir,
            f"debug_infos_{str(datetime.datetime.now()).replace(' ', '_')}.json",
        )
        with open(filename, "w") as f:
            pprint(debug_infos, stream=f)
        ret_filename = os.path.join(
            output_dir,
            f"ret_data_{str(datetime.datetime.now()).replace(' ', '_')}.json",
        )
        with open(ret_filename, "w") as f:
            pprint(rets, stream=f)
        pprint(f"完成！debug 与返回信息已存入 {output_dir} 下.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="SR Probius")
    parser.add_argument(
        "-c",
        "--conf",
        default=None,
        help="指定配置 yaml 的位置，不指定时使用默认的 /home/sa_cluster/sr/conf/probius_conf.yaml 路径.",
    )
    parser.add_argument(
        "-o", "--output_dir", default="./", help="输出 debug 信息的目录，默认为当前目录."
    )
    parser.add_argument(
        "-p", "--project_ref_id", required=True, type=int, help="项目ref_id"
    )
    parser.add_argument(
        "-q", "--quick", action="store_true", help="直接使用 conf 中的第一个值进行请求."
    )
    parser.add_argument(
        "-i", "--input", action="store_true", help="直接输入 url 和 json 即可访问."
    )
    parser.add_argument(
        "-ko",
        "--keyoption",
        nargs="*",
        help=(
            "输入 {key}={number} 来快速进行参数选择，key 代表参数，number 代表位置."
            + "ex. -ko distinct_id=0 item_type=1"
        ),
    )
    parser.add_argument(
        "-kv",
        "--keyval",
        nargs="*",
        help=(
            "输入 {key}={val} 来快速增加参数及其值，key 代表参数，val 代表值."
            + "ex. -kv distinct_id=abcd123 item_type=article"
        ),
    )
    args = parser.parse_args()

    probius = Probius(
        conf_path=args.conf,
        quick=args.quick,
        _input=args.input,
        key_option=args.keyoption,
        key_val=args.keyval,
        ref_id=args.project_ref_id
    )
    probius.run(output_dir=args.output_dir)
