#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @File  : sr_colored_str.py
# @Author: DingYan
# @Date  : 2020/5/20

COLOR = {
    'HEADER': '\033[95m',
    'BLUE': '\033[94m',
    'INFO': '\033[92m',
    'WARN': '\033[93m',
    'ERROR': '\033[91m',
    'BOLD': '\033[1m',
    'UNDERLINE': '\033[1m',
    'END': '\033[0m'
}


class SrColored(object):

    @staticmethod
    def colored_str(style, s):
        s = str(s)
        if style not in COLOR:
            raise ValueError("Invalid style , u can add it by yourself")
        return COLOR[style] + s + COLOR['END']


if __name__ == "__main__":
    print(SrColored.colored_str('BLUE', 'blue'))
