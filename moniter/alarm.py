#!/usr/bin/env python
# -*- coding: utf-8 -*-
import requests
import json

def get_access_token(app_key, app_secret):
    url = f"https://oapi.dingtalk.com/gettoken?appkey={app_key}&appsecret={app_secret}"
    response = requests.get(url)
    return response.json()['access_token']

def send_dingtalk_message(access_token, agent_id, user_id_list, message):
    url = f"https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2"
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

# 使用示例
app_key = 'dingbfew2tpzrm0atojk'
app_secret = 'T5_2ucmzao8Zi7vYsTerT5Xtd0Nt3dQoncPnG8Yxv72f0jl5f-XDRrZMMmLg92mL'
agent_id = '3176723147'
user_id_list = '160422383935775689'
message = '报警信息内容'

access_token = get_access_token(app_key, app_secret)
result = send_dingtalk_message(access_token, agent_id, user_id_list, message)

print(result)