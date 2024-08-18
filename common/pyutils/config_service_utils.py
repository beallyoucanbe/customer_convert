#! --*-- coding: utf-8 --*--

import os
import sys
import random
import socket
import json

cur_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = os.path.join(cur_dir, os.path.pardir, os.path.pardir)
sys.path.append(root_dir)
sys.path.append(os.path.join(root_dir, "proto_lib"))

from google.protobuf.json_format import MessageToDict
from common.pyutils.discovery_utils import get_config_service_conf
from common.grpc_service.config_service_client import ConfigServiceClient
from hyperion_client.config_manager import ConfigManager
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy import INT
from sqlalchemy import Index
from sqlalchemy import Column
from sqlalchemy import VARCHAR
from sqlalchemy import TEXT
from sqlalchemy import DATETIME
from sqlalchemy.ext.declarative import declarative_base
import redis

Base = declarative_base()


class SrSystemConfig(Base):
    # 项目名字相关信息
    __tablename__ = 'sr_system_config'

    # 表的结构:
    id = Column(INT(), primary_key=True, autoincrement=True)
    project_id = Column(INT())
    item_type_name = Column(VARCHAR(64))
    name = Column(VARCHAR(64))
    cname = Column(VARCHAR(64))
    content = Column(TEXT())
    create_time = Column(DATETIME())
    update_time = Column(DATETIME())

    Index("system_config", project_id, item_type_name, name)


DBSession = None
pool = None


def get_config_detail(sr_conf_details):
    beta_config_d = {}
    prod_config_d = {}
    for conf_detail in sr_conf_details:
        key = conf_detail.get("type") + "_" + conf_detail.get("name")
        if conf_detail.get("is_beta") and check_if_beta(conf_detail.get("hosts")):
            beta_config_d[key] = conf_detail
        else:
            prod_config_d[key] = conf_detail
    # 遍历灰度配置, 根据key直接添加到全量配置中
    for key in beta_config_d:
        prod_config_d[key] = beta_config_d.get(key)
    return {"config_details": list(prod_config_d.values())}


def get_sr_conf_detail(project_id=None, name=None, version_id=None):
    """ 获取项目的栏位配置

    """
    sr_conf_detail = None
    if version_id:
        return get_config_by_version_id(project_id=project_id, version_id=version_id)
    try:
        session = init_session()
        config_model = session.query(SrSystemConfig).filter_by(project_id=project_id,
                                                               name='config_center_cache_host').first()
        if config_model:
            redis_host = config_model.content
            redis_pool = create_redis_pool(redis_host)
            conn = redis.Redis(connection_pool=redis_pool, decode_responses=True)
            key = f'{project_id}_project_all_config'
            sr_conf_cache = conn.get(key)
            sr_conf_detail = json.loads(str(sr_conf_cache, 'UTF-8'))
            sr_conf_detail = json.loads(sr_conf_detail)
            sr_conf_details = sr_conf_detail.get("config_details", [])
            return get_config_detail(sr_conf_details)
    except Exception as e:
        print("get config from cache failed", e)
        sr_conf_detail = None
        pass
    if not sr_conf_detail:
        all_service_conf = get_config_service_conf()
        if not all_service_conf:
            raise Exception("not enough alive config_service. please check it !")
        conf = random.choice(all_service_conf)
        host = conf["address"]
        port = conf["port"]
        params = {
            "project_id": project_id,
            "name": name,
            "version_id": version_id
        }
        client = ConfigServiceClient(rpc_host=host, rpc_port=port)
        response = client.ListConfigDetail(params)
        data = MessageToDict(response, preserving_proto_field_name=True)
        # 对灰度配置进行筛选
        sr_conf_details = data.get("config_details", [])
        return get_config_detail(sr_conf_details)
    return sr_conf_detail


def get_sr_conf_summary(project_id=None, name=None, only_online_status=True, limit=5):
    """ 获取项目的栏位配置

    """
    # host = "10.120.34.247"
    # port = "8380"
    all_service_conf = get_config_service_conf()
    if not all_service_conf:
        raise Exception("not enough alive config_service. please check it !")
    print(f"all_service_conf: {all_service_conf}")
    conf = random.choice(all_service_conf)
    host = conf["address"]
    port = conf["port"]
    params = {
        "project_id": project_id,
        "name": name,
        "only_online_status": only_online_status,
        "limit": limit
    }
    client = ConfigServiceClient(rpc_host=host, rpc_port=port)
    response = client.ListConfigSummary(params)
    data = MessageToDict(response, preserving_proto_field_name=True)
    return data


def get_config_by_version_id(project_id=None, name=None, version_id=None):
    """ 根据version id 获取配置

    """
    all_service_conf = get_config_service_conf()
    if not all_service_conf:
        raise Exception("not enough alive config_service. please check it !")
    conf = random.choice(all_service_conf)
    host = conf["address"]
    port = conf["port"]
    params = {
        "project_id": project_id,
        "name": name,
        "version_id": version_id
    }
    client = ConfigServiceClient(rpc_host=host, rpc_port=port)
    response = client.ListConfigDetail(params)
    data = MessageToDict(response, preserving_proto_field_name=True)
    return data


def get_sr_section_and_scene_conf(project_id=None):
    get_sr_conf_detail(project_id=project_id)


def check_if_beta(hosts=None):
    """
    需要检验ip,hostname,全限定域名
    :param hosts:
    :return:
    """
    current_hostname = socket.gethostname()
    current_ip = socket.gethostbyname(current_hostname)
    current_fqdn = socket.getfqdn()
    if hosts:
        raw_hosts = [x for x in hosts.strip().split(',') if x]
        hosts = []
        for raw_host in raw_hosts:
            hosts.append(raw_host.strip())
        for host in hosts:
            if current_hostname == host or current_ip == host or current_fqdn == host:
                return True
    return False


def init_session():
    global engine
    global DBSession

    if not DBSession:
        # 初始化数据库连接:
        # mysql_conf = {
        #     'host': 'localhost',
        #     'port': '3306',
        #     'dbname': 'sr_web_service',
        #     'user': 'root',
        #     'password': ''
        # }

        # url = 'mysql+mysqldb://%(user)s:%(password)s@%(host)s:%(port)s/%(dbname)s' % mysql_conf

        cm = ConfigManager()
        mysql_conf = {}
        jdbc_url_list = cm.get_client_conf_by_key(product_name='sp',
                                                  module_name='mysql',
                                                  key='jdbc_url_list')
        master_index = cm.get_client_conf_by_key(product_name='sp',
                                                 module_name='mysql',
                                                 key='master_index')
        mysql_conf['password'] = cm.get_client_conf_by_key(product_name='sp',
                                                           module_name='mysql',
                                                           key='password')
        mysql_conf['user'] = cm.get_client_conf_by_key(product_name='sp',
                                                       module_name='mysql',
                                                       key='user')
        mysql_conf['host'] = jdbc_url_list[master_index].split('//')[1].split('/')[0].split(':')[0]
        mysql_conf['port'] = int(jdbc_url_list[0].split('//')[1].split('/')[0].split(':')[1])
        engine = create_engine(
            'mysql+pymysql://%(user)s:%(password)s@%(host)s:%(port)s/metadata' % mysql_conf,
            echo=mysql_conf.get('echo', False),
            pool_size=50,
            pool_recycle=3600,
            pool_pre_ping=True,
            connect_args={'charset': 'utf8'},
        )
        DBSession = sessionmaker(bind=engine)
        Base.metadata.create_all(engine)

    return DBSession()


def create_redis_pool(address):
    global pool

    if not pool:
        host = address.split(':')[0]
        port = address.split(':')[1]
        default_password = "MhxzKhl2015"
        pool = redis.ConnectionPool(host=host, port=port, password=default_password, max_connections=10)

    return pool


if __name__ == "__main__":
    sr_conf_detail = get_sr_conf_detail(project_id=1, version_id=2)
    sr_conf_details = sr_conf_detail.get("config_details", [])
    section_confs = []
    for conf_detail in sr_conf_details:
        if conf_detail.get("type") == "section":
            section_confs.append(conf_detail.get("value"))
    print(section_confs)
