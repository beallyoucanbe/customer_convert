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
    message = datetime.now().strftime("%Y-%m-%d %H:%M:%S") + ": " + message
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

def send_nginx_log_alarm(data):
    # {"response_time_result": {"response_time_tp80": 0.051, "response_time_tp99": 0.502, "response_time_average": 0.033, "response_time_tp90": 0.084}, "code_dict": {"200": 39, "502": 1, "500": 5}}
    response_time_tp80 = 0
    if 'response_time_result' in data and 'response_time_tp80' in data['response_time_result']:
        response_time_tp80 = data['response_time_result']['response_time_tp80']
    err_request = 0
    if 'code_dict' in data:
        for key, value in data['code_dict'].items():
            if not (key.startswith('2') or key.startswith('3')):
                err_request = err_request + value

    if response_time_tp80 > 0.2 or err_request > 10:
        # 发送报警信息
        send_dingtalk_message(json.dumps(data))

if __name__ == '__main__':
    data = {"response_time_result": {"response_time_tp80": 0.051, "response_time_tp99": 0.502, "response_time_average": 0.033, "response_time_tp90": 0.084}, "code_dict": {"200": 39, "502": 1, "500": 5}}
    result = send_nginx_log_alarm(data)
