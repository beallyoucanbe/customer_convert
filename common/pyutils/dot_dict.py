# coding: utf-8

import traceback
from collections import OrderedDict


class DotDict(object):
    def __init__(self, *args, **kwargs):
        self._data = OrderedDict()

        if args:
            d = args[0]
            if isinstance(d, DotDict):
                self._data = d._data
            if isinstance(d, dict):
                for k, v in d.items():
                    if isinstance(v, dict):
                        v = DotDict(v)
                    if type(v) is list:
                        l = []
                        for i in v:
                            n = i
                            if type(i) is dict:
                                n = DotDict(i)
                            l.append(n)
                        v = l
                    self._data[k] = v

        if kwargs:
            for k, v in kwargs.items():
                self._data[k] = v

    def items(self):
        return self._data.items()

    def __iter__(self):
        return self._data.__iter__()

    # tmp comment
    # def next(self):
    #     return self._data.next()

    def __setitem__(self, key, value):
        self._data[key] = value

    def __getitem__(self, k):
        return self._data[k]

    def __setattr__(self, k, v):
        if k in {'_data'}:
            super(DotDict, self).__setattr__(k, v)
        else:
            self[k] = v

    # ????
    def __getattr__(self, item):
        if item in {'_data'}:
            raise KeyError('invalid key _data')
            # super(DotDict, self).__getattr__(item)
        else:
            return self[item]

    def __str__(self):
        items = []
        for k, v in self._data.items():
            if id(v) == id(self):
                items.append('{0}=DotDict(...)'.format(k))
            else:
                items.append('{0}={1}'.format(k, repr(v)))
        joined = ', '.join(items)
        out = '{0}({1})'.format(self.__class__.__name__, joined)
        return out

    def __repr__(self):
        return str(self)

    def __delattr__(self, k):
        return self._data.__delitem__(key)

    def __contains__(self, k):
        return self._data.__contains__(k)

    def keys(self):
        return self._data.keys()

    def __dir__(self):
        return self.keys()

    def __len__(self):
        return self._data.__len__()

    def merge(self, b_dict):
        try:
            for k, v in b_dict.items():
                if k in self:
                    if isinstance(v, dict) or isinstance(v, DotDict):
                        if isinstance(self[k], DotDict):
                            self[k] = self[k].merge(v)
                        else:
                            self[k] = DotDict(v)
                    else:
                        self._data.update(b_dict)
                else:
                    if isinstance(v, dict) or isinstance(v, DotDict):
                        self[k] = DotDict(v)
                    else:
                        self[k] = v
            return self
        except:
            print(traceback.format_exc())
            print(self, '---', b_dict)
            print(k, '---', self[k], '---', v)

    def to_dict(self):
        d = {}
        for k, v in self.items():
            if type(v) is DotDict:
                if id(v) == id(self):
                    v = d
                else:
                    v = v.to_dict()
            elif type(v) in (list, tuple):
                l = []
                for i in v:
                    n = i
                    if type(i) is DotDict:
                        n = i.to_dict()
                    l.append(n)
                if type(v) is tuple:
                    v = tuple(l)
                else:
                    v = l
            d[k] = v
        return d


if __name__ == "__main__":
    eg = {"a": {"b": 1, "c": 2}, "d": 3}
    x = DotDict(eg)
