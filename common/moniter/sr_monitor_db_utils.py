#! --*-- coding: utf-8 --*--

import datetime

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

Base = declarative_base()


class SrMonitor(Base):
    # 项目名字相关信息
    __tablename__ = 'sr_monitor'

    # 表的结构:
    id = Column(INT(), primary_key=True, autoincrement=True)
    project_name = Column(VARCHAR(128))
    type = Column(VARCHAR(64))
    name = Column(VARCHAR(64))
    instance = Column(VARCHAR(128))
    content = Column(TEXT())
    create_time = Column(DATETIME())
    alarm = Column(INT())

    Index("monitor_create_time", create_time)


DBSession = None


def save_sr_monitor_to_db(project_name, type, name, instance, content):
    """ 保存monitor 信息到数据库

    """
    try:
        session = init_session()
        sr_monitor = SrMonitor(
            project_name=project_name,
            type=type,
            name=name,
            instance=instance,
            content=content
        )
        sr_monitor.create_time = datetime.datetime.now()
        sr_monitor.alarm = 0
        session.add(sr_monitor)
        session.commit()
    except Exception as e:
        print("get config from cache failed", e)


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


if __name__ == "__main__":
    save_sr_monitor_to_db(project_name='default', type='biz_redis', name='slow log', instance='hybrid01',
                          content='1234')
