#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : data_box.py
# @Author: DingYan
# @Date  : 2019/2/19
# @Desc  :
import numpy as np


class WebStatusBox(object):
    def __init__(self):
        box_list = ["request", "recall", "rank", "rerank", "model"]
        for i in box_list:
            setattr(self, i, StatusBox(i))


class StatusBox(object):
    def __init__(self, box_name):
        self.name = box_name
        self.content = {}

    def __repr__(self):
        return str(self.content)

    def set(self, key, value):
        self.content[key] = value

    def set_mean(self, key, _list):
        self.content[key] = np.mean(_list)

    def set_count(self, key, _list):
        self.content[key] = sum(_list)

    def set_min(self, key, _list):
        self.content[key] = min(_list)

    def set_max(self, key, _list):
        self.content[key] = max(_list)

    def set_99pt(self, key, _list):
        self.content[key] = np.percentile(_list, 99)

    def set_90pt(self, key, _list):
        self.content[key] = np.percentile(_list, 90)

    def set_list_statistics(self, key, _list):
        """
        将list中的数值算出各种统计值set进box
        :param key:
        :param _list:
        :return:
        """
        statistics_list = ['mean', 'count', 'min', 'max', '90pt', '99pt']
        for opt in statistics_list:
            getattr(self, 'set_' + opt)(key + '_' + opt, _list)

    def set_dict(self, _dict):
        self.content.update(_dict)

    def set_statistic_dict(self, _dict):
        """
        如果value是列表会生成统计信息
        :param _dict:
        :return:
        """
        for i in _dict:
            if isinstance(_dict[i], list):
                self.set_list_statistics(i, _dict[i])
            else:
                self.set(i, _dict[i])

    def get(self, key):
        if self.content.__contains__(key):
            return self.content[key]
        else:
            return None

    def delete(self, key):
        if self.content.__contains__(key):
            del self.content[key]

    def pop(self, key):
        if self.content.__contains__(key):
            return self.content.pop(key)

    def incr(self, key, step=1):
        if self.content.__contains__(key):
            self.content[key] += step
            return self.content[key]
        else:
            self.content[key] = 1

    def clear(self):
        self.content = {}
