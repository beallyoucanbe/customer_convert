#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import sys
from typing import Dict
import prometheus_client
from prometheus_client.registry import CollectorRegistry

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../'))


class PrometheusHelper(object):

    def __init__(self):
        from hyperion_client.deploy_topo import DeployTopo
        host_list = DeployTopo().get_host_list_by_module_name('sm', 'pushgateway')
        self.pushgateway_host = host_list[0] + ":8315"

    def init_redis_prometheus(self, redis_gauge_dict, registry):
        documentation = 'check_redis_info'
        redis_gauge_dict['sr_redis_connected_clients'] = prometheus_client.Gauge(
            'sr_redis_connected_clients',
            documentation,
            ['instance', 'product_component', 'project', 'module'],
            registry=registry)
        redis_gauge_dict['sr_redis_memory_max_bytes'] = prometheus_client.Gauge(
            'sr_redis_memory_max_bytes',
            documentation,
            ['instance', 'product_component', 'project', 'module'],
            registry=registry)
        redis_gauge_dict['sr_redis_memory_used_bytes'] = prometheus_client.Gauge(
            'sr_redis_memory_used_bytes',
            documentation,
            ['instance', 'product_component', 'project', 'module'],
            registry=registry)
        redis_gauge_dict['sr_redis_keyspace_hits_total'] = prometheus_client.Gauge(
            'sr_redis_keyspace_hits_total',
            documentation,
            ['instance', 'product_component', 'project', 'module'],
            registry=registry)
        redis_gauge_dict['sr_redis_keyspace_misses_total'] = prometheus_client.Gauge(
            'sr_redis_keyspace_misses_total',
            documentation,
            ['instance', 'product_component', 'project', 'module'],
            registry=registry)
        redis_gauge_dict['sr_redis_commands_processed_total'] = prometheus_client.Gauge(
            'sr_redis_commands_processed_total',
            documentation,
            ['instance', 'product_component', 'project', 'module'],
            registry=registry)
        redis_gauge_dict['sr_redis_net_input_bytes_total'] = prometheus_client.Gauge(
            'sr_redis_net_input_bytes_total',
            documentation,
            ['instance', 'product_component', 'project', 'module'],
            registry=registry)
        redis_gauge_dict['sr_redis_net_output_bytes_total'] = prometheus_client.Gauge(
            'sr_redis_net_output_bytes_total',
            documentation,
            ['instance', 'product_component', 'project', 'module'],
            registry=registry)

    def init_nginx_prometheus(self, nginx_gauge_dict, registry):
        documentation = 'check_nginx_info'
        nginx_gauge_dict['sr_web_service_request_time_tp80'] = prometheus_client.Gauge(
            'sr_web_service_request_time_tp80',
            documentation,
            ['instance', 'product_component', 'project', 'module'],
            registry=registry)
        nginx_gauge_dict['sr_web_service_request_time_tp90'] = prometheus_client.Gauge(
            'sr_web_service_request_time_tp90',
            documentation,
            ['instance', 'product_component', 'project', 'module'],
            registry=registry)
        nginx_gauge_dict['sr_web_service_request_time_tp99'] = prometheus_client.Gauge(
            'sr_web_service_request_time_tp99',
            documentation,
            ['instance', 'product_component', 'project', 'module'],
            registry=registry)
        nginx_gauge_dict['sr_web_service_request_time_eve'] = prometheus_client.Gauge(
            'sr_web_service_request_time_eve',
            documentation,
            ['instance', 'product_component', 'project', 'module'],
            registry=registry)
        nginx_gauge_dict['sr_web_service_request'] = prometheus_client.Gauge(
            'sr_web_service_request',
            documentation,
            ['instance', 'product_component', 'project', 'module', 'code'],
            registry=registry)

    def init_dataflow_prometheus(self, dataflow_gauge_dict, registry):
        documentation = 'check_dataflow_info'
        dataflow_gauge_dict['sr_dataflow_status'] = prometheus_client.Gauge(
            'sr_dataflow_status',
            documentation,
            ['product_component', 'project', 'module', 'dataflow_name', 'status', 'current_step'],
            registry=registry)
        dataflow_gauge_dict['sr_dataflow_consumed_time_second'] = prometheus_client.Gauge(
            'sr_dataflow_consumed_time_second',
            documentation,
            ['product_component', 'project', 'module', 'dataflow_name', 'status'],
            registry=registry)
        dataflow_gauge_dict['sr_dataflow_write_to_redis_count'] = prometheus_client.Gauge(
            'sr_dataflow_write_to_redis_count',
            documentation,
            ['product_component', 'project', 'module', 'dataflow_name', 'status'],
            registry=registry)

    def do_monitor_redis(self, project_name, redis_info_dict, cache_type):
        registry = CollectorRegistry()
        redis_gauge_dict: Dict[str, prometheus_client.Gauge] = {}
        self.init_redis_prometheus(redis_gauge_dict, registry)
        for instance in redis_info_dict:
            self.check_redis_info(redis_gauge_dict, instance, project_name, redis_info_dict[instance], cache_type)
        prometheus_client.push_to_gateway(self.pushgateway_host, job='sr-redis-' + cache_type + '-' + project_name, registry=registry)

    def check_redis_info(self, redis_gauge_dict, instance, project_name, redis_info, cache_type):
        redis_gauge_dict.get("sr_redis_connected_clients").labels(instance, 'sr', project_name, "redis-" + cache_type).set(redis_info['connected_clients'])
        redis_gauge_dict.get("sr_redis_memory_max_bytes").labels(instance, 'sr', project_name, "redis-" + cache_type).set(redis_info['maxmemory'])
        redis_gauge_dict.get("sr_redis_memory_used_bytes").labels(instance, 'sr', project_name, "redis-" + cache_type).set(redis_info['used_memory'])
        redis_gauge_dict.get("sr_redis_keyspace_hits_total").labels(instance, 'sr', project_name, "redis-" + cache_type).set(redis_info['keyspace_hits'])
        redis_gauge_dict.get("sr_redis_keyspace_misses_total").labels(instance, 'sr', project_name, "redis-" + cache_type).set(redis_info['keyspace_misses'])
        redis_gauge_dict.get("sr_redis_commands_processed_total").labels(instance, 'sr', project_name, "redis-" + cache_type).set(redis_info['total_commands_processed'])
        redis_gauge_dict.get("sr_redis_net_input_bytes_total").labels(instance, 'sr', project_name, "redis-" + cache_type).set(redis_info['total_net_input_bytes'])
        redis_gauge_dict.get("sr_redis_net_output_bytes_total").labels(instance, 'sr', project_name, "redis-" + cache_type).set(redis_info['total_net_output_bytes'])

    def do_monitor_nginx(self, instance, project_name, nginx_info):
        registry = CollectorRegistry()
        nginx_gauge_dict: Dict[str, prometheus_client.Gauge] = {}
        self.init_nginx_prometheus(nginx_gauge_dict, registry)
        self.check_nginx_info(nginx_gauge_dict, instance, project_name, nginx_info)
        prometheus_client.push_to_gateway(self.pushgateway_host, job='sr-nginx-' + instance + '-' + project_name, registry=registry)

    def check_nginx_info(self, nginx_gauge_dict, instance, project_name, nginx_info):
        if nginx_info['response_time_result']:
            nginx_gauge_dict.get("sr_web_service_request_time_tp80").labels(instance, 'sr', project_name, "sr_nginx").set(nginx_info['response_time_result']['response_time_tp80'])
            nginx_gauge_dict.get("sr_web_service_request_time_tp90").labels(instance, 'sr', project_name, "sr_nginx").set(nginx_info['response_time_result']['response_time_tp90'])
            nginx_gauge_dict.get("sr_web_service_request_time_tp99").labels(instance, 'sr', project_name, "sr_nginx").set(nginx_info['response_time_result']['response_time_tp99'])
            nginx_gauge_dict.get("sr_web_service_request_time_eve").labels(instance, 'sr', project_name, "sr_nginx").set(nginx_info['response_time_result']['response_time_average'])
        if nginx_info['code_dict']:
            for code in nginx_info['code_dict']:
                nginx_gauge_dict.get("sr_web_service_request").labels(instance, 'sr', project_name, "sr_nginx", code).set(nginx_info['code_dict'][code])

    def do_monitor_dataflow(self, dataflow_info_dict):
        """
        上报数据流结果的各项指标
        {
            "project_name": "product__1__adidas",
            "dataflow_name": "item_cf",
            "consumer_time": "30",
            "status": "success",
            "write_redis_num": 1000,
            "err_message":""
        }
        :param dataflow_info_dict:
        :return:
        """
        registry = CollectorRegistry()
        dataflow_gauge_dict: Dict[str, prometheus_client.Gauge] = {}
        self.init_dataflow_prometheus(dataflow_gauge_dict, registry)
        self.check_dataflow_info(dataflow_gauge_dict, dataflow_info_dict)
        project_name = dataflow_info_dict['project_name']
        dataflow_name = dataflow_info_dict['dataflow_name']
        prefix = 'srm' if 'srm' in dataflow_name else ''
        prometheus_client.push_to_gateway(self.pushgateway_host, job='sr-dataflow-' + prefix + '-' + project_name, registry=registry)

    def check_dataflow_info(self, dataflow_gauge_dict, dataflow_info_dict):

        project_name = dataflow_info_dict['project_name']
        dataflow_name = dataflow_info_dict['dataflow_name']
        status = dataflow_info_dict['status']
        step = dataflow_info_dict['current_step']
        dataflow_gauge_dict.get("sr_dataflow_status").labels('sr', project_name, "redis-dataflow", dataflow_name, status, step).set(1 if status == 'success' else 0)
        if 'consumer_time' in dataflow_info_dict:
            dataflow_gauge_dict.get("sr_dataflow_consumed_time_second").labels('sr', project_name, "redis-dataflow", dataflow_name, status).set(dataflow_info_dict['consumer_time'])
        if 'write_redis_num' in dataflow_info_dict:
            dataflow_gauge_dict.get("sr_dataflow_write_to_redis_count").labels('sr', project_name, "redis-dataflow", dataflow_name, status).set(dataflow_info_dict['write_redis_num'])
