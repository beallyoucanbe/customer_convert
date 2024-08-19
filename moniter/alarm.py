#!/usr/bin/env python
# -*- coding: utf-8 -*-
import requests
import json
from datetime import datetime, time

app_key = 'dingbfew2tpzrm0atojk'
app_secret = 'T5_2ucmzao8Zi7vYsTerT5Xtd0Nt3dQoncPnG8Yxv72f0jl5f-XDRrZMMmLg92mL'
agent_id = '3176723147'
user_id_list = '160422383935775689,076434076732437917'

def get_access_token(app_key, app_secret):
    url = "https://oapi.dingtalk.com/gettoken?appkey=" + app_key + "&appsecret=" + app_secret
    response = requests.get(url)
    return response.json()['access_token']

def send_dingtalk_message(message):
    access_token = get_access_token(app_key, app_secret)
    url = "https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2"
    headers = {'Content-Type': 'application/json'}
    data = {
        "agent_id": agent_id,
        "userid_list": user_id_list,
        "msg": {
            "msgtype": "text",
            "text": {
                "content": message
            }
        }
    }
    response = requests.post(url, headers=headers, data=json.dumps(data), params={'access_token': access_token})
    return response.json()

def is_working_hour():
    now = datetime.now()
    current_time = now.time()
    if now.weekday() < 5:  # 周一到周五是工作日
        if time(9, 0) <= current_time <= time(12, 0) or time(13, 0) <= current_time <= time(20, 0):
            return True
    return False

def send_customer_log_alarm(data):
    # 通话量小于5 或者 报错数大于3
    if data["success"] < 5 or data["error"] > 3:
        # 判断当前时间
        if is_working_hour():
            message = '过去半小时的通话量为：' + str(data["success"]) + '，报错数为：' + str(data["error"])
            send_dingtalk_message(message)

if __name__ == '__main__':
    message = '报警信息1111内容'
    result = send_customer_log_alarm({"success":4, "error":5})
