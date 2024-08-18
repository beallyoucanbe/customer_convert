#! --*-- coding: utf-8 --*--

import os
import json
import random
import logging

from .service_discovery_client import ServiceDiscoveryClient, ServiceInstance, ServiceType

from hyperion_client.config_manager import ConfigManager
from hyperion_client.hyperion_inner_client.inner_config_manager import InnerConfigManager
from hyperion_guidance.zk_connector import ZKConnector
from hyperion_element.exceptions import IaasElementException

log = logging.getLogger(__name__)

# ========= 一些hook 方法，服务发现相关，先项目内实现，验证过后看是否需要迁移入 sp 代码库 =========

"""
config_manger -> inner_config_manager -> zk_connector

hyperion_client.config_manager.ConfigManager -->
hyperion_client.hyperion_inner_client.inner_config_manager.InnerConfigManager -->

给 ConfigManager 和 InnerConfigManager 分别新增一个 get_discovery_conf 函数
"""


def config_manger_hook():
    """ 给 ConfigManger 动态挂载一些函数

    Args:
        ConfigManger:

    Returns:

    """

    def get_discovery_conf(self, server_type):
        """ 挂在 config_manager.ConfigManager 上

        Args:
            self:
            server_type:

        Returns:
            [{'name': 'ai_calc_engine_service',
              'id': '3144fde3-086f-4c55-9cc5-cc466f1263fd',
              'address': 'hybrid01.debugtestreset14545.sensorsdata.cloud',
              'port': 8382,
              'ssl_port': None,
              'payload': None,
              'registration_time_utc': 1619793997558,
              'service_type': 'DYNAMIC',
              'uri_spec': None}
            ]
        """
        conf = self.inner_config_manager.get_discovery_conf(server_type=server_type)
        return conf

    def register_service(self, server_type, service_instance: ServiceInstance):
        """ 挂在 config_manager.ConfigManager 上

        Args:
            self:
            server_type:
            service_instance:

        Returns:

        """
        return self.inner_config_manager.register_service(server_type, service_instance)

    ConfigManager.get_discovery_conf = get_discovery_conf
    ConfigManager.register_service = register_service


def inner_config_manager_hook():
    def get_discovery_conf(self, server_type):
        """ 挂在 InnerConfigManager 上
        Args:
            server_type: 服务类型
        Returns:
            [dict client_conf, dict client_conf]
        """
        # 直接操作 zk discovery_client 取数
        client = ZkBasedServiceDiscoveryClient.create_instance(server_type)
        instances = client.get_all_instance()
        return instances

    def register_service(self, server_type, service_instance: ServiceInstance):
        """ 挂在 InnerConfigManager 上
        Args:
            server_type: 服务类型
            service_instance: 服务实例
        Returns:

        """
        # 直接操作 zk discovery_client 取数
        client = ZkBasedServiceDiscoveryClient.create_instance(server_type)
        return client.register_service(service_instance)

    InnerConfigManager.get_discovery_conf = get_discovery_conf
    InnerConfigManager.register_service = register_service


config_manger_hook()
inner_config_manager_hook()


# ========= 一些 hook 方法，服务发现相关，先项目内实现，验证过后迁移入sp 代码库 =========

class ZkBasedServiceDiscoveryClient(ServiceDiscoveryClient):
    """ useage:

        zk_service_discovery_client = ZkBasedServiceDiscoveryClient.create_instance("ai_based_recommend_service")

        service_instance = ServiceInstance(
            name="ai_based_recommend_service",
            id=str(uuid.uuid1()),
            address=socket.getfqdn(),
            port=9527,
        )
        zk_service_discovery_client.register_service(service_instance)

    """
    cache = {}
    ZK_DISCOVERY_PATH = "discovery"

    def __init__(self, service_type, logger=None):
        """

        Args:
            service_type: 服务的类型，如 data_loader_worker，注意：只允许小写字母、数字、减号和下划线
        """
        self.logger = logger or log
        self.service_type = service_type
        self.client = ZKConnector.get_instance()
        self.zk_client = self.client.get_zk_client()
        self.service_path = self.client.join_full_path(self.ZK_DISCOVERY_PATH, service_type)
        self.logger.info(f"ZkBasedServiceDiscoveryClient init, service_path: {self.service_path}")

    @classmethod
    def create_instance(cls, service_type, logger=None):
        if service_type not in cls.cache:
            cls.cache[service_type] = cls(service_type, logger=logger)
        return cls.cache[service_type]

    def create_discovery_by_path(self, zk_path, default_value=None, **kwargs):
        """
        创建zk的一个节点,并赋上值,默认会初始化成 {}
        会递归的创建父级目录
        :param zk_path: 绝对路径
        :param default_value:
        :return:
        """
        if default_value is None:
            default_value = {}
        if type(default_value) is not dict:
            raise IaasElementException(
                "create_by_path type(value) is not dict [type(value): %s]" % type(default_value))

        zk = self.zk_client
        if zk.exists(zk_path):
            raise Exception('Path[%s] already exist on zk!' % zk_path)

        # 父目录可能也不存在 需要创建
        zk_parent_path = os.path.dirname(zk_path)
        zk.ensure_path(zk_parent_path)
        # 判断节点类型
        ephemeral = default_value.get("service_type") == ServiceType.DYNAMIC
        zk.create(zk_path, json.dumps(default_value).encode(encoding="utf-8"), ephemeral=ephemeral)
        self.logger.info(f"create_discovery_by_path: {zk_path}, {ephemeral} ")

    def set_json_value_by_discovery_path(self, zk_path, value: dict):
        """ 如果节点不存在则创建
        :param zk_path: 绝对路径
        :param value:
        """
        if type(value) is not dict:
            raise IaasElementException(
                "set_json_value_by_path type(value) is not dict [type(value): %s]" % type(value))
        if not self.client.check_config_by_path(zk_path):
            self.create_discovery_by_path(zk_path, default_value=value)
        else:
            zk = self.zk_client
            if not zk.exists(zk_path):
                raise Exception('Path[%s] does not exist on zk!' % zk_path)
            zk.set(zk_path, json.dumps(value).encode(encoding="utf-8"))

    def register_service(self, service_instance: ServiceInstance):
        """

        Args:
            service_instance:

        Returns:

        """
        service_path = self.service_path
        if not self.client.exists(service_path):
            self.client.create_by_path(service_path)

        conf = service_instance.to_dict()
        child_path = os.path.join(self.service_path, service_instance.id)
        self.set_json_value_by_discovery_path(child_path, conf)

    def unregister_service(self, service_instance: ServiceInstance):
        """

        Args:
            service_instance:

        Returns:

        """
        service_path = self.service_path
        if not self.client.exists(service_path):
            return

        child_path = os.path.join(self.service_path, service_instance.id)
        return self.zk_client.delete(child_path)

    def update_service(self, service_instance: ServiceInstance):
        """

        Args:
            service_instance:

        Returns:

        """
        service_path = self.service_path
        if not self.client.exists(service_path):
            self.client.create_by_path(service_path)

        conf = service_instance.to_dict()
        child_path = os.path.join(self.service_path, service_instance.id)
        self.set_json_value_by_discovery_path(child_path, conf)

    def get_instance(self):
        """ 获取服务一个实例配置

        Returns:
                {
                  "name" : "ai_calc_engine_service",
                  "id" : "2769aacf-3396-4781-a050-507ae317d723",
                  "address" : "hybrid01.debugtestreset14545.sensorsdata.cloud",
                  "port" : 8382,
                  "ssl_port" : null,
                  "payload" : null,
                  "registration_time_utc" : 1620622945339,
                  "service_type" : "DYNAMIC",
                  "uri_spec" : null
                }
        """
        res = self.get_all_instance()
        return random.choice(res) if res else None

    def get_all_instance(self):
        """ 获取服务所有实例配置

        Returns:
            [
                {
                  "name" : "ai_calc_engine_service",
                  "id" : "2769aacf-3396-4781-a050-507ae317d723",
                  "address" : "hybrid01.debugtestreset14545.sensorsdata.cloud",
                  "port" : 8382,
                  "ssl_port" : null,
                  "payload" : null,
                  "registration_time_utc" : 1620622945339,
                  "service_type" : "DYNAMIC",
                  "uri_spec" : null
                }
            ]
        """
        res = []
        service_path = self.service_path
        if not self.client.exists(service_path):
            return res

        children = self.client.get_children_by_path(service_path)
        for i in children:
            child_path = os.path.join(service_path, i)
            conf = self.client.get_json_value_by_path(child_path)
            res.append(conf)
        return res

    def add_listener(self):
        pass
