#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : influxdb_helper.py.py
# @Author: DingYan
# @Date  : 2019/2/12
# @Desc  : influxdb 相关的一切

import time

from influxdb import InfluxDBClient


class InfluxDBHelper(object):
    def __init__(self, conf):
        self.message_list = []
        self.influx_conf = conf
        self.influx_dbname = self.influx_conf['dbname']
        self.influx_transform_type = self.influx_conf['type']
        self.influx_host = self.influx_conf['host']
        self.influx_port = int(self.influx_conf['port'])
        if self.influx_transform_type == 'udp':
            self.influx_client = InfluxDBClient(host=self.influx_host, database=self.influx_dbname,
                                                udp_port=self.influx_port, use_udp=1, timeout=0.01)
        else:
            self.influx_client = InfluxDBClient(host=self.influx_host, port=self.influx_port,
                                                database=self.influx_dbname, timeout=10)
            try:
                self.influx_client.create_database(self.influx_dbname)
            except Exception:
                pass

    def _send(self, message_list, rp='2_hours'):
        if self.influx_client:
            self.influx_client.write_points(points=message_list, retention_policy=rp)

    def dataflow_stage(self, box_dict):
        """
        不需要统计信息的数据流
        :param box_dict:
        :return:
        """
        tags = ['flow_name']
        fields = list(box_dict.content.keys())
        dataflow_dict = {"measurement": box_dict.get('flow_name'), "tags": dict([(x, box_dict.get(x)) for x in tags]),
                         "fields": dict([(x, box_dict.get(x)) for x in fields]),
                         "time": int(time.time() * 1000000000)}
        return dataflow_dict

    def dataflow_monitor_stage(self, box_dict):
        """
        该方法是对从pipline中经过sent2influxdb方法计算后的返回结果进行处理。目的是调整结果的格式符合influxdb的要求。
        :param box_dict:是pipline中经过sent2influxdb方法计算后的结果
        :return dataflow_dict：是经过格式调整后，发到influxdb的结果
        """
        dataflow_dict = {"measurement": box_dict.get('measurement_name'), "time": box_dict.get('time'),
                         "tags": box_dict.get('tags'),
                         "fields": box_dict.get('fields')
                         }
        return dataflow_dict

    def request_stage(self, box_dict):
        request_tags = ["host", "service_port", "exp_id", "code", "strategy_id", "scene_id", "category", "semoniter"]
        request_fields = ["request_log_id", "request_item_id", "request_limit", "request_distinct_id",
                          "total_time_cost", "res_num"]
        request_dict = {"measurement": "request",
                        "tags": dict([(x, box_dict.request.get(x)) for x in request_tags]),
                        "fields": dict([(x, box_dict.request.get(x)) for x in request_fields])
                        }
        request_dict["fields"].update(box_dict.request.get("retrieve_id_cnt"))
        request_dict["fields"].update(box_dict.request.get("item_type_cnt"))
        # request_dict["fields"].update({"calc_fm_scores_time":box_dict.request.get("calc_fm_scores_time")})
        request_dict["time"] = int(time.time() * 1000000000)
        return request_dict

    def recall_stage(self, box_dict):
        recall_tags = ["host", "service_port", "exp_id", "strategy_id", "scene_id", "category", "semoniter"]
        recall_fields = ["request_log_id", "item_info_null_cnt"]
        recall_dict = {"measurement": "recall",
                       "tags": dict([(x, box_dict.request.get(x)) for x in recall_tags]),
                       "fields": dict(
                           [(x, box_dict.rank.get(x)) if box_dict.recall.get(x) else (x, box_dict.request.get(x)) for x
                            in
                            recall_fields]),
                       }

        if box_dict.recall.get("retrieve_id_cnt"):
            recall_dict["fields"].update(box_dict.recall.get("retrieve_id_cnt"))
        if box_dict.recall.get("item_type_cnt"):
            recall_dict["fields"].update(box_dict.recall.get("item_type_cnt"))
        recall_dict["time"] = int(time.time() * 1000000000)
        return recall_dict

    def rank_stage(self, box_dict):

        rank_tags = ["host", "service_port", "exp_id", "strategy_id", "scene_id", "category", "semoniter"]
        rank_fields = ["request_log_id", 'rank_exception']
        rank_dict = {"measurement": "rank",
                     "tags": dict([(x, box_dict.request.get(x)) for x in rank_tags]),
                     "fields": dict(
                         [(x, box_dict.rank.get(x)) if box_dict.rank.get(x) else (x, box_dict.request.get(x)) for x in
                          rank_fields]),
                     }
        if box_dict.rank.get("null_feature_cnt"):
            rank_dict['fields'].update(box_dict.rank.get("null_feature_cnt"))
        if box_dict.rank.get("mean_feature"):
            rank_dict['fields'].update(box_dict.rank.get("mean_feature"))
        rank_dict["time"] = int(time.time() * 1000000000)
        return rank_dict

    def rerank_stage(self, box_dict):
        tags = ["host", "service_port", "exp_id", "strategy_id", "scene_id", "category", "semoniter"]
        fields = ["request_log_id", 'rerank_exception']
        data = {"measurement": "rerank",
                "tags": dict([(x, box_dict.request.get(x)) for x in tags]),
                "fields": dict(
                    [(x, box_dict.rerank.get(x)) if box_dict.rerank.get(x) else (x, box_dict.request.get(x)) for x in
                     fields]),
                }
        data["time"] = int(time.time() * 1000000000)
        return data

    def model_stage(self, box_dict):
        model_tags = ["model_type", "model_name", "host", "port"]
        model_fields = ["time_cost", "log_id"]
        model_dict = {"measurement": "model",
                      "tags": dict([(x, box_dict.model.get(x)) for x in model_tags]),
                      "fields": dict(
                          [(x, box_dict.model.get(x)) for x in
                           model_fields]),
                      }
        model_dict["time"] = int(time.time() * 1000000000)
        return model_dict

    def send(self, box_dict, rp='2_hours'):
        """
        将box_dict分阶段生成发送给influxdb
        :return:
        """

        # 请求级别
        request_message = self.request_stage(box_dict)
        self.message_list.append(request_message)

        # 召回阶段
        recall_message = self.recall_stage(box_dict)
        self.message_list.append(recall_message)

        # 排序阶段
        rank_message = self.rank_stage(box_dict)
        self.message_list.append(rank_message)

        # 重排序阶段
        rerank_message = self.rerank_stage(box_dict)
        self.message_list.append(rerank_message)

        # 发送
        self._send(message_list=self.message_list, rp=rp)
