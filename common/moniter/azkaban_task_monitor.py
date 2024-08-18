#! --*-- coding: utf-8 --*--

import os
import sys
from datetime import datetime
import time
import logging
from logging import handlers
import yaml
import glob

cur_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = os.path.join(cur_dir, os.path.pardir, os.path.pardir)
sys.path.append(root_dir)
from common.pyutils.sr_alarm_utils import sr_alarm
from common.pyutils.sensors_recommend_config_utils import get_sr_log_path
from common.pyutils import sensors_recommend_config_utils

from hyperion_client.config_manager import ConfigManager
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy import INT
from sqlalchemy import Column
from sqlalchemy import VARCHAR
from sqlalchemy import BIGINT
from sqlalchemy import BOOLEAN
from sqlalchemy import and_
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()

SR_LOG_PATH = get_sr_log_path()

## 数据流的超时报警时间，默认1h，超过该时间没执行完会触发报警。如果自己配置了，读取配置文件
dataflow_timeout_default = 60 * 60 * 1000


class ExecutionFlows(Base):
    # 项目名字相关信息
    __tablename__ = 'execution_flows'

    # 表的结构:
    exec_id = Column(INT(), primary_key=True, autoincrement=True)
    project_id = Column(INT())
    version = Column(INT())
    flow_id = Column(VARCHAR(128))
    status = Column(INT())
    submit_user = Column(VARCHAR(64))
    submit_time = Column(BIGINT())
    update_time = Column(BIGINT())
    start_time = Column(BIGINT())
    end_time = Column(BIGINT())
    enc_type = Column(INT())
    executor_id = Column(INT())
    # 这里是原计划用来监控azkaban 任务状态的，抛弃azkaban 自带的报警插件，直接通过定时检查数据库的方式来监控
    # alarm = Column(INT())


class AzkabanProjects(Base):
    # 项目名字相关信息
    __tablename__ = 'projects'

    # 表的结构:
    id = Column(INT(), primary_key=True, autoincrement=True)
    name = Column(VARCHAR(64))
    active = Column(BOOLEAN())
    modified_time = Column(BIGINT())
    create_time = Column(BIGINT())
    version = Column(INT())
    last_modified_by = Column(VARCHAR(64))
    description = Column(VARCHAR(2048))
    enc_type = Column(INT())


DBSession = None
pool = None


def get_project_by_id(project_id):
    session = init_session()
    return session.query(AzkabanProjects).filter_by(id=project_id).first()

def get_project_by_name(project_name):
    session = init_session()
    return session.query(AzkabanProjects).filter(and_(AzkabanProjects.name == project_name, AzkabanProjects.active == 1)).first()

def exec_exector_flow_failed_monitor(logger=None):
    """ 获取运行失败的flow
    RUNNING(30),
    SUCCEEDED(50),
    KILLED(60),
    FAILED(70),

    """
    try:
        session = init_session()
        current_timestamp = (int(round(time.time() * 1000)))
        timestamp_five_mins = current_timestamp - 5 * 60 * 1000
        exector_flows = session.query(ExecutionFlows).filter(
            and_(ExecutionFlows.end_time > timestamp_five_mins, ExecutionFlows.status == 70,
                 ExecutionFlows.alarm == 0)).all()
        exec_alarm(exector_flows, logger)
        # 报警发送完成之后，更新报警的标志位
        for flow in exector_flows:
            flow.alarm = 1
        session.commit()
    except Exception as e:
        logger.error(f"check azkaban error: {e}")
        return


def exec_exector_flow_timeout_monitor(project_name, logger=None):
    """ 获取超时失败的flow
    RUNNING(30),
    SUCCEEDED(50),
    KILLED(60),
    FAILED(70),

    """
    try:
        session = init_session()
        current_timestamp = (int(round(time.time() * 1000)))
        timeout_time, azkaban_project_name = getTimeoutTimeAndProjectName(project_name)
        timestamp_threshold = current_timestamp - timeout_time
        azkaban_project = get_project_by_name(azkaban_project_name)
        if not azkaban_project:
            return
        exector_flows = session.query(ExecutionFlows).filter(
            and_(ExecutionFlows.project_id == azkaban_project.id, ExecutionFlows.start_time < timestamp_threshold, ExecutionFlows.status == 30)).all()
        exec_alarm(azkaban_project_name, exector_flows, 'timeout', logger)
    except Exception as e:
        logger.error(f"check azkaban error: {e}")
        return


def exec_alarm(azkaban_project_name, exector_flows_failed, type, logger):
    if not exector_flows_failed or len(exector_flows_failed) < 1:
        return
    error_project_message = ''
    for flow in exector_flows_failed:
        flow_error = "Antiy Azkaban 3.0 工作流执行状态: {} \n".format(type)
        flow_error += "工作流项目名称为：{}--{},目前已经执行{}!\n".format(azkaban_project_name, flow.flow_id, type)
        flow_error += "Execution ID: {}\n".format(flow.exec_id)
        flow_error += "运行开始时间: {}\n".format(getTiemStr(flow.start_time))
        if type == 'timeout':
            flow_error += "共耗时：{} sec\n".format(round(time.time()) - round(flow.start_time / 1000))
        else:
            flow_error += "运行结束时间: {}\n".format(getTiemStr(flow.end_time))
            flow_error += "共耗时：{}\n".format(flow.end_time - flow.start_time)
        error_project_message += flow_error
    print(error_project_message)
    logger.info(f"project {azkaban_project_name} dataflow alarm: {error_project_message}")
    title = "SR_DATAFLOW_FAIL: " + azkaban_project_name
    sr_alarm('FATAL', title, error_project_message)


def getTiemStr(time_long):
    timeStamp = int(time_long) / 1000
    return datetime.fromtimestamp(timeStamp).strftime("%Y-%m-%d %H:%M:%S")


def getTimeoutTimeAndProjectName(project_name):
    dataflow_timeout = dataflow_timeout_default
    # 默认用sr项目名称
    azkaban_project_name = project_name
    sr_recommend_home = sensors_recommend_config_utils.get_sr_home_path()
    dataflow_schedule_conf = os.path.join(sr_recommend_home,
                                          ConfigManager().get_product_global_conf("sr").get('company_name'),
                                          "dataflow_schedule_conf.yaml")
    if not dataflow_schedule_conf or not os.path.exists(dataflow_schedule_conf):
        return dataflow_timeout, azkaban_project_name
    filename, ext = os.path.splitext(dataflow_schedule_conf)
    for conf_file in glob.iglob(f"{filename}*{ext}"):
        try:
            with open(conf_file, "rb") as f:
                part_conf = yaml.load(f, Loader=yaml.Loader)
            for azkaban_project_name_conf, ap_conf in part_conf.items():
                # 这里只读取指定的sr 项目的配置
                project_name_conf = ap_conf.get("belong_to")
                if project_name_conf and project_name_conf == project_name:
                    azkaban_project_name = azkaban_project_name_conf
                    timeout_minutes = ap_conf.get("timeout_minutes")
                    if timeout_minutes:
                        dataflow_timeout = timeout_minutes * 60 * 1000
                    break
        except Exception:
            return dataflow_timeout, azkaban_project_name
    return dataflow_timeout, azkaban_project_name


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
        mysql_conf['password'] = 'SensorsRecommend2017'
        mysql_conf['user'] = 'azkaban'
        mysql_conf['host'] = jdbc_url_list[master_index].split('//')[1].split('/')[0].split(':')[0]
        mysql_conf['port'] = int(jdbc_url_list[0].split('//')[1].split('/')[0].split(':')[1])
        engine = create_engine(
            'mysql+pymysql://%(user)s:%(password)s@%(host)s:%(port)s/azkaban' % mysql_conf,
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
    project_name = sys.argv[1]
    type = sys.argv[2]
    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)
    moniter_log_path = os.path.join(SR_LOG_PATH, 'moniter')
    logFilePath = f"{moniter_log_path}/tiny_moniter.log"
    errorFilePath = f"{moniter_log_path}/tiny_moniter.error"

    fa = handlers.RotatingFileHandler(logFilePath, 'a', 1024 * 1024, 10)
    fa.setLevel(logging.INFO)
    formater = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    fa.setFormatter(formater)
    logger.addHandler(fa)

    fe = handlers.RotatingFileHandler(errorFilePath, 'a', 1024 * 1024, 10)
    fe.setLevel(logging.ERROR)
    formater = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    fe.setFormatter(formater)
    logger.addHandler(fe)
    logger.info('start monitor azkaban executions...')
    if 'timeout' in type:
        exec_exector_flow_timeout_monitor(project_name, logger)
