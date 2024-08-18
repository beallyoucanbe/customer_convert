#!/home/sa_cluster/sr/python/bin/python3
"""模型服务的压力测试工具.

需要在 PressTool 中增加对应 model service 的函数，例如 fm_init.
xxx_init: 负责返回 client 和处理好的 data，并给 self.press 函数赋值
self.press: 一次请求的操作，输入为 client 和 data

注意：
1. 同一线程内的两次请求之间如果响应时间过慢，会造成永远达不到指定 rps 的情况，此时已经达到其压力上限。
"""
import asyncio
import argparse
import logging
import os
import sys
import time
from random import choices, randint
from threading import Lock, Thread

import numpy as np

__version__ = "0.0.2"

sys.path.append(os.path.join(os.path.abspath(os.path.dirname(__file__)), '../../'))


class PressTool(object):

    def __init__(self, name, host, port, rps, concurrent, test_time):
        self.host = host
        self.port = port
        self.rps = rps
        self.concurrent = max(concurrent, 1)
        self.test_time = test_time
        self.sleep_time = 1 / (self.rps / self.concurrent) if self.rps > 0 else 0
        self.cnt_lock = Lock()
        self.err_lock = Lock()
        self.cnt = 0
        self.err_cnt = 0
        self.client, self.data = getattr(self, f"{name}_init")()
        # 记录每次请求耗时
        self.press_time = list()

    def worker_manager(self):
        last_time_index = 0
        for i in range(self.test_time):
            time.sleep(1)
            with self.cnt_lock:
                cur_rps = self.cnt
                self.cnt = 0
            if self.sleep_time > 0:
                if cur_rps < self.rps:
                    self.sleep_time *= 0.95
                elif cur_rps > self.rps:
                    self.sleep_time *= 1.05
            with self.err_lock:
                err_cnt = self.err_cnt
                self.err_cnt = 0
            now_time_index = len(self.press_time)
            tl = self.press_time[last_time_index:now_time_index]
            last_time_index = now_time_index
            print(f"rps: {cur_rps}, eps: {err_cnt}, mean: {np.mean(tl)}, 99: {np.percentile(tl, 99)}")

    def worker_func(self):
        start_time = time.time()
        client = self.client(rpc_host=self.host, rpc_port=self.port, logger=logging.getLogger())
        data_len = len(self.data)
        data_index = randint(0, data_len - 1)
        while True:
            with self.cnt_lock:
                self.cnt += 1
            data_index += 1
            time.sleep(self.sleep_time)
            s_time = time.time()
            try:
                self.press(client, self.data[data_index % data_len])
            except Exception:
                with self.err_lock:
                    self.err_cnt += 1
            e_time = time.time()
            self.press_time.append(e_time - s_time)
            if e_time - start_time > self.test_time:
                break

    def fm_init(self):
        """返回 Client 类和测试数据，并给 self.press 赋值."""
        from common.model_service.fm.client import FMClient
        from itertools import product
        import pickle
        with open('data_info.pkl', 'rb') as f:
            data = pickle.load(f)
        user_id = choices(list(data['user'].keys()), k=200)
        channels = list(data['channels'].keys())
        tags = choices(list(data['tags'].keys()), k=100)
        item_list = list()
        for c, t in product(channels, tags):
            display_count = randint(0, 1000)
            click_count = randint(0, display_count)
            browse_count = randint(0, display_count)
            other_count = randint(0, display_count)
            item_list.append(['', c, t, display_count, click_count, browse_count, other_count, other_count, other_count,
                              other_count])
        data = list()
        for i in range(1000):
            data.append({'user_id': user_id[i % 200], 'items': choices(item_list, k=800),
                         'ctr_pre': {'yule': 0.2, 'jiankan': 0.3}})

        self.loop = asyncio.get_event_loop()

        def press(client, data):
            self.loop.run_until_complete(client.predict(data))

        self.press = press
        return FMClient, data

    def dnn_init(self):
        """返回 Client 类和测试数据，并给 self.press 赋值."""
        from common.model_service.dnn.client import DNNClient

        print('Read test data start...')
        data = list()
        cnt = 0
        with open('small_test_data', 'rb') as f:
            line = f.readline().strip()
            while line:
                data.append(eval(line))
                cnt += 1
                line = f.readline().strip()
        print('Read test data done. Count: ', cnt)

        self.loop = asyncio.get_event_loop()

        def press(client, data):
            self.loop.run_until_complete(client.predict(data))

        self.press = press
        return DNNClient, data

    def run(self):
        threads = [Thread(target=self.worker_func) for _ in range(self.concurrent)]
        threads.append(Thread(target=self.worker_manager))
        for t in threads:
            t.start()
        for t in threads:
            t.join()
        print("mean: ", np.mean(self.press_time))
        print("99%: ", np.percentile(self.press_time, 99))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Model Service Benchmark Tool")
    parser.add_argument('-n', '--name', required=True, help="model file name, such as hmf_v2, gbdt and fm.")
    parser.add_argument('-H', '--host', default='localhost', help="model service host, default: localhost.")
    parser.add_argument('-p', '--port', required=True, help="model service port.")
    parser.add_argument('-r', '--rps', default=20, type=int, help="request per second.")
    parser.add_argument('-c', '--concurrent', default=10, type=int, help="request concurrent number.")
    parser.add_argument('-t', '--time', default=120, type=int, help="test time (second).")
    args = parser.parse_args()
    w = PressTool(args.name, args.host, args.port, args.rps, args.concurrent, args.time)
    w.run()
