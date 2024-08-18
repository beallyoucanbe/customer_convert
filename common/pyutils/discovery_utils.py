#! --*-- coding: utf-8 --*--

import os
import sys

cur_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = os.path.join(cur_dir, os.path.pardir, os.path.pardir)
sys.path.append(root_dir)
sys.path.append(os.path.join(root_dir, "proto_lib"))

from hyperion_client.config_manager import ConfigManager
from common.pyutils.discovery.zk_service_discovery_client import ZkBasedServiceDiscoveryClient  # noqa

# 内部服务定义, 用于服务发现注册
CONFIG_SERVICE = "cos-grpc-server"


def get_config_service_conf():
    """

    Returns:
        [
            {
              "name" : "cos-grpc-server",
              "id" : "4b9fcc97-b879-466a-a5a7-1e7c3dc2c7bf",
              "address" : "hybrid01.debugresetreset15518.sensorsdata.cloud",
              "port" : 8100,
              "ssl_port" : null,
              "payload" : {"service_name":"cos-grpc-server","port":8100},
              "registration_time_utc" : 1620380109875,
              "service_type" : "DYNAMIC",
              "uri_spec" : null
            }
        ]
    """
    return ConfigManager().get_discovery_conf(CONFIG_SERVICE)


if __name__ == "__main__":
    print(ConfigManager().get_discovery_conf("cos-grpc-server"))
