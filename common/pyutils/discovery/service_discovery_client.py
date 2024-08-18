#! --*-- coding: utf-8 --*--

import uuid
from abc import ABC


class ServiceType(object):
    DYNAMIC = "DYNAMIC"
    STATIC = "STATIC"
    PERMANENT = "PERMANENT"


class ServiceInstance(object):
    def __init__(self, name="", id="", address=None, port=None, ssl_port=None, payload=None, registration_time_utc=0,
                 service_type=ServiceType.DYNAMIC, uri_spec=None):
        self.name = name
        # 实例的 ID，如果需要区别不同实例，可以设置有意义的值如 instance03，如果不需要区分，可以设置 UUID
        self.id = id or str(uuid.uuid1())

        # 服务提供接口访问的地址 IP/机器名，如 data02.sa
        self.address = address
        # 服务提供接口的端口号，如 8106
        self.port = port
        self.ssl_port = ssl_port
        # 业务相关的信息
        self.payload = payload
        # 该服务注册的时间
        self.registration_time_utc = registration_time_utc
        self.service_type = service_type
        self.uri_spec = uri_spec

    def to_dict(self):
        return {
            "name": self.name,
            "id": self.id,
            "address": self.address,
            "port": self.port,
            "ssl_port": self.ssl_port,
            "payload": self.payload,
            "registration_time_utc": self.registration_time_utc,
            "service_type": self.service_type,
            "uri_spec": self.uri_spec,
        }

    @classmethod
    def parse_from_dict(cls, conf):
        instance = cls(**conf)
        return instance


class ServiceDiscoveryClient(ABC):

    def register_service(self):
        """注册一个服务实例

        Returns:

        """
        raise NotImplementedError

    def unregister_service(self):
        """注销一个服务实例

        Returns:

        """
        raise NotImplementedError

    def update_service(self):
        """更新一个服务描述

        Returns:

        """
        raise NotImplementedError

    def get_instance(self):
        """获取一个服务的实例

        Returns:

        """
        raise NotImplementedError

    def get_all_instance(self):
        """获取该服务所有实例

        Returns:

        """
        raise NotImplementedError

    def add_listener(self):
        """添加一个对服务实例变动的监听

        Returns:

        """
        raise NotImplementedError
