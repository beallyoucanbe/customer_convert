#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : __init__.py.py
# @Author: DingYan dingyan@sensorsdata.cn
# @Date  : 2020/8/31

import os
import sys

# 适配 sp 2.0.0 加入 armada path
ARMADA_HOME = os.environ.get("ARMADA_HOME", "")
sys.path.append(f"{ARMADA_HOME}/hyperion")
