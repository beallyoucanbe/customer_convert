# -*- coding: utf-8 -*-
"""
slog helper, support notice log

SLogger
    实现了notice方法的logger
SLoggerAdapter
    SLoggerAdapter(logger, extra={'hash_key': 'xxx'}), 其notice方法可不用每次都自己带上hash_key了.

"""

__all__ = [
    "set_logger",
    "SLoggerAdapter",
    "RequestIdLoggerAdapter",
]

import os
import sys
import json
import gzip
import shutil
import logging
import numpy as np

from logging import StreamHandler
from logging.handlers import TimedRotatingFileHandler

# add level NOTICE(25), between INFO(25) and WARN(30)
NOTICE = 25
logging.addLevelName(NOTICE, "NOTICE")
logging.NOTICE = 25
logging.basicConfig(format='%(asctime)s %(funcName)s:%(lineno)d %(levelname)s %(message)s', datefmt="%Y-%m-%dT%H:%M:%S")

print('# add notice log level', logging.NOTICE)


class NpEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, np.integer):
            return int(obj)
        elif isinstance(obj, np.floating):
            return float(obj)
        elif isinstance(obj, np.ndarray):
            return obj.tolist()
        else:
            return super(NpEncoder, self).default(obj)


class CronTimedRotatingFileHandler(TimedRotatingFileHandler):
    def computeRollover(self, currentTime):
        # 按小时的整点切割
        if self.when == 'H':
            currentTime = currentTime // 3600 * 3600
        result = super(CronTimedRotatingFileHandler, self).computeRollover(currentTime)
        return result


class SLogger(logging.Logger):
    """
    Logger Class support NOTICE LOG (logger.notice func)
    """

    def __init__(self, name):
        super(SLogger, self).__init__(name)
        self._collector_dict = {}

    def notice(self, hash_key, key="", value="", notice_cmd="push", *args, **kwargs):
        """
        notice log func, merge several logs with same hash_key
        :param hash_key:
        :param key:
        :param value:
        :param notice_cmd: enum[push/flush]
        :param args:
        :param kwargs:
        :return:
        """
        if self.isEnabledFor(NOTICE):
            self.push_notice(hash_key, key, value)
            if notice_cmd == "flush":
                msg = self.flush_notice(hash_key)
                # if hash_key not exist, return msg is None
                if msg is not None:
                    self._log(NOTICE, msg, args, **kwargs)

    def push_notice(self, hash_key, key, value):
        """
        add logs to collector_dict
        :param hash_key:
        :param key:
        :param value:
        :return:
        """
        if not key:
            return
        r_key = self.independent_key(key, self._collector_dict.setdefault(hash_key, {}))
        self._collector_dict.setdefault(hash_key, {})[r_key] = value

    def flush_notice(self, hash_key, json_dump=True):
        """
        extract logs from collector_dict
        :param hash_key:
        :param json_dump:
        :return:
        """
        if hash_key in self._collector_dict:
            msg = self._collector_dict.pop(hash_key)
            if json_dump:
                msg = json.dumps(msg, cls=NpEncoder, separators=(',', ':'))
            return msg
        else:
            return ""

    @staticmethod
    def independent_key(key, k_dict):
        """
        rewrite key if duplicated in k_dict
        add _[num] behind the key, up to 100.
        :param key:
        :param k_dict:
        :return:
        """
        if key not in k_dict:
            return key
        else:
            for i in range(1, 100):
                d_key = key + "_" + str(i)
                if d_key not in k_dict:
                    return d_key
            return None


class RequestIdLoggerAdapter(logging.LoggerAdapter):
    def __init__(self, logger, extra):
        super(RequestIdLoggerAdapter, self).__init__(logger, extra)
        self.extra = extra

    def add_extra(self, extra):
        self.extra.update(extra)

    def warning(self, msg, *args, **kwargs):
        self.logger.warning('log_id: %s. %s' % (self.extra['log_id'], msg), *args, **kwargs)

    def error(self, msg, *args, **kwargs):
        self.logger.error('log_id: %s. %s' % (self.extra['log_id'], msg), *args, **kwargs)


class SLoggerAdapter(logging.LoggerAdapter):
    """
    SLogger adapter, use without adding hash_key all the time.
    extra: {'hash_key': 'xxx', ...}
    """

    def __init__(self, logger, extra):
        super(SLoggerAdapter, self).__init__(logger, extra)
        self.extra = extra

    def add_extra(self, extra):
        self.extra.update(extra)

    def notice(self, key="", value="", notice_cmd="push", *args, **kwargs):
        self.logger.notice(self.extra['hash_key'], key, value, notice_cmd, *args, **kwargs)


# funcs for compress
def compress_namer(name):
    """
    logger handler compress namer
    :param name:
    :return:
    """
    return name + ".gz"


tar_cmd = None


def get_tar_cmd():
    global tar_cmd
    if tar_cmd is None:
        p = os.popen('which tar')
        tar_cmd = p.read().strip()
        p.close()
    return tar_cmd


def compress_rotator(source, dest):
    """
    logger handler compress rotator
    :param source:
    :param dest:
    :return:
    """
    # shell 异步压缩
    cmd = get_tar_cmd()
    if cmd:
        # logging rotator 后会重新 open(source, 'w') 写日志文件，同步模式 rotator -> rm -> open 没问题
        # shell 异步执行，压缩需要时间，可能执行流程 rotator -> open -> rm, 把 logging 新 open 的日志文件句柄给删了
        # 改个临时文件名，压缩后再删除临时文件
        tmp_source = source + '.tmp'
        os.rename(source, tmp_source)
        source = tmp_source
        os.system('%(cmd)s -zcvf %(dest)s %(source)s && rm %(source)s &' % {'source': source, 'dest': dest, 'cmd': cmd})
    else:
        with open(source, 'rb') as f_in:
            with gzip.open(dest, 'wb') as f_out:
                shutil.copyfileobj(f_in, f_out)

        os.remove(source)


class SLogHelper(object):
    """
    方便以conf定义logger
    """

    _default_fmt = "[%(asctime)s] [%(levelname)s] [%(funcName)s:%(lineno)d] %(message)s"
    _default_datefmt = "%Y-%m-%dT%H:%M:%S"
    _default_propagate = True
    _default_logger_level = 'NOTSET'
    _default_handler_level = 'NOTSET'

    _default_time_rotating_file_conf = {'when': 'D',
                                        'interval': 1,
                                        'backup_count': 5,
                                        'compress': False}

    @classmethod
    def set_logger(cls, logger, config):
        logger.propagate = config.get("propagate", cls._default_propagate)
        logger.setLevel(level=config.get("level", cls._default_logger_level))
        for hdlr_config in config.get("handlers", []):
            cls.add_handler(logger, hdlr_config)

    @classmethod
    def add_handler(cls, logger, config):
        """
        限定 type in ('time_rotating_file', 'stream')
        :param logger:
        :param config:
        :return:
        """
        hdlr_type = config.get("type")
        if hdlr_type in ["time_rotating_file", "cron_time_rotating_file"]:
            filepath = config.get("filepath")
            if not filepath:
                return
            when = config.get("when", cls._default_time_rotating_file_conf['when'])
            interval = config.get("interval", cls._default_time_rotating_file_conf['interval'])
            backup_count = config.get("backup_count", cls._default_time_rotating_file_conf['backup_count'])

            # if hdlr_type == 'time_rotating_file':
            #     hdlr = TimedRotatingFileHandler(filepath, when, interval, backup_count)
            # else:
            #     hdlr = CronTimedRotatingFileHandler(filepath, when, interval, backup_count)

            hdlr = CronTimedRotatingFileHandler(filepath, when, interval, backup_count)

            compress = config.get("compress", cls._default_time_rotating_file_conf['compress'])
            if compress:
                hdlr.rotator = compress_rotator
                hdlr.namer = compress_namer

        elif hdlr_type == "stream":
            stream = config.get("stream")
            if not stream:
                stream = sys.stdout
            hdlr = StreamHandler(stream)
        else:
            return

        # set formatter
        fs = config.get("format", cls._default_fmt)
        dfs = config.get("datefmt", cls._default_datefmt)
        fmt = logging.Formatter(fs, dfs)
        hdlr.setFormatter(fmt)

        # set level
        level = config.get("level", cls._default_handler_level)
        hdlr.setLevel(level)

        # add filter
        if config.get("filter_config"):
            action = config["filter_config"].get("action")
            action_sets = set([getattr(logging, x) for x in config["filter_config"].get("sets", [])])
            if action == "filter" and action_sets:
                log_filter = logging.Filter()
                log_filter.filter = lambda record: record.levelno not in action_sets
                hdlr.addFilter(log_filter)
            elif action == "reserved" and action_sets:
                log_filter = logging.Filter()
                log_filter.filter = lambda record: record.levelno in action_sets
                hdlr.addFilter(log_filter)

        # add handler
        logger.addHandler(hdlr)

    @staticmethod
    def clear_handlers(logger):
        logger.handlers = []


# todo: check 是否有必要
logging.getLogger().setLevel("NOTSET")
logging.setLoggerClass(SLogger)
print('Set logger class to Slogger.')


def set_logger(name, config):
    """
    expose this func to initialize the logger
    :param name:
    :param config:
    :return:
    """
    _logger = logging.getLogger(name)
    SLogHelper.set_logger(_logger, config)
    return _logger
