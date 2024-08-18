# -*- coding: utf-8 -*-

import zlib

# https://en.wikipedia.org/wiki/List_of_file_signatures
# 通过数据头判断是否为zlib数据

zlib_magic_headers = {
    b'x\x01',   # level: 0 - 1
    b'x^',      # level: 2 - 5
    b'x\x9c',   # level: 6 or -1
    b'x\xda',   # level: 7 - 9
}


def encrypt_data(data):
    """
    :param data:
    :return:
    """
    if not data:
        return data
    if isinstance(data, str):
        data = data.encode('utf-8')
    res = zlib.compress(data)
    # print('encrypt_data effect. rate: %s' % (len(res) / len(data)))
    return res


def decrypt_data(data):
    """判断是否zlib压缩数据，是的话解码

    :param data:
    :return:
    """
    if not data:
        return data
    res = data
    if data[:2] in zlib_magic_headers:
        res = zlib.decompress(data)
    return res
