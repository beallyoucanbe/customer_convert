#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : AlertServer.py
# @Author: DingYan
# @Date  : 2019/3/18
# @Desc  : xjb搞得接grafana报警的server
import sys
from http.server import SimpleHTTPRequestHandler, HTTPServer
import json
import os

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../../'))
sys.path.append(os.path.join(os.environ.get("ARMADA_HOME", ""), 'hyperion'))

try:
    # sp 1.18
    from sp.pycommon.utils.sa_utils import alarm_info, alarm_warn, alarm_fatal
except Exception:
    # sp 2.0 +
    from hyperion_client.alarm import Alarm

    alarm = Alarm()
    alarm_info = alarm.info
    alarm_warn = alarm.warn
    alarm_fatal = alarm.fatal


def sr_alarm(level, title, msg):
    msg = msg
    if level.upper() == 'INFO':
        alarm_info(title, msg)
    if level.upper() == 'WARN':
        alarm_warn(title, msg)
    if level.upper() == 'FATAL':
        alarm_fatal(title, msg)


class RequestHandler(SimpleHTTPRequestHandler):
    def do_POST(self):
        raw_data = self.rfile.read(int(self.headers['content-length']))
        data = json.loads(raw_data)
        title = 'SR_' + data['ruleName']
        if data['state'] == 'ok':
            title = title + '_OK'
        sr_alarm('WARN', title, raw_data)


def run():
    port = 9400
    print('starting server, port', port)
    # Server settings
    server_address = ('', port)
    httpd = HTTPServer(server_address, RequestHandler)
    print('running server...')
    httpd.serve_forever()


if __name__ == '__main__':
    run()
