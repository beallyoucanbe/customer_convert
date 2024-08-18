#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : sr_alarm_utils.py
# @Author: DingYan
# @Date  : 2020/4/14
# @Desc  : 触发sp报警的工具

import os
import sys

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../../'))
sys.path.append(os.path.join(os.environ.get("ARMADA_HOME", ""), 'hyperion'))

try:
    # sp 2.0 +
    from hyperion_client.alarm import Alarm
    alarm = Alarm()
    alarm_info = alarm.info
    alarm_warn = alarm.warn
    alarm_fatal = alarm.fatal

except Exception:
    # sp 1.18
    from sp.pycommon.utils.sa_utils import alarm_info, alarm_warn, alarm_fatal


def sr_alarm(level, title, msg, product='SR', module='DATAFLOW'):
    msg = '\n' + msg
    if level.upper() == 'INFO':
        alarm_info(title, msg, module, product)
    if level.upper() == 'WARN':
        alarm_warn(title, msg, module, product)
    if level.upper() == 'FATAL':
        alarm_fatal(title, msg, module, product)
