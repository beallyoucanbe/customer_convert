#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : alarm.py.py
# @Author: DingYan
# @Date  : 2019/4/28
# @Desc  : 数据流报警通过azkaban报警插件触发

import os
import sys
import traceback

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../"))

from common.pyutils.sr_alarm_utils import sr_alarm
from common.pyutils.sensors_recommend_config_utils import (
    get_company_name,
    get_sr_home_path,
)
from common.pyutils.yaml_utils import read_yaml

dataflow_alarm_config = {
    "category_topk": "fatal",
    "hmf_flow": "fatal",
    "doc2vec_flow": "fatal",
    "must_rec": "warn",
    "item_weight": "warn",
    "content_profile": "fatal",
    "user_profile": "fatal",
}

if __name__ == "__main__":
    try:
        alarm_msg = sys.argv[1]
        data_flow_name = alarm_msg.split("\n")[1].split("：")[1].split(",")[0]
        finish_time = alarm_msg.split("\n")[4].split("时间:")[1]
        # 尝试读取公司配置下的数据流报警配置并update到默认配置上
        company_name = get_company_name()
        sr_home_path = get_sr_home_path()
        custome_alarm_config_path = os.path.join(
            sr_home_path, company_name, "conf", "moniter", "dataflow_alarm_config.yaml"
        )
        if os.path.exists(custome_alarm_config_path):
            custome_alarm_config = read_yaml(custome_alarm_config_path)
            dataflow_alarm_config = dataflow_alarm_config.update(custome_alarm_config)

        if data_flow_name.lower() in dataflow_alarm_config:
            if dataflow_alarm_config[data_flow_name] == "fatal":
                sr_alarm(
                    "fatal",
                    "SR_DATAFLOW_FAIL: " + data_flow_name + "_" + finish_time,
                    alarm_msg,
                )
            elif dataflow_alarm_config[data_flow_name] == "warn":
                sr_alarm("warn", "SR_DATAFLOW_FAIL: " + data_flow_name, alarm_msg)
        else:
            sr_alarm(
                "fatal",
                "SR_DATAFLOW_FAIL: " + data_flow_name + "_" + finish_time,
                alarm_msg,
            )
        print("java 调用有第三方库的python脚本成功" + alarm_msg)
    except Exception:
        print(traceback.format_exc())
