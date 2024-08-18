# coding:utf-8

# 计算连续特征的信息增益

import sys
import json
from absl import flags

FLAGS = flags.FLAGS
flags.DEFINE_string('data_path', None, 'data file path.')
flags.DEFINE_string('feature', None, 'feature')
flags.DEFINE_integer('cut', -1, 'average cut bounder')


class FeatureCount():
    def __init__(self, file_path, feature_from, cut=-1):
        print("Start read file...")
        with open(file_path) as f:
            self.data = f.readlines()
            f.close()
        print("Done. Size: ", len(self.data))
        self.feature_from = feature_from
        self.cut = cut

    def get_data(self, data, get_from, default_value):
        idx = get_from.find('.')
        now = get_from[:idx] if idx >= 0 else get_from
        rest = get_from[idx + 1:] if idx >= 0 else ''
        if now not in data:
            return default_value
        return self.get_data(data[now], rest, default_value) if rest else data[now]

    def do_cnt(self):
        tot = len(self.data)
        hit = 0
        feature_map = {}
        for d in self.data:
            d = json.loads(d)
            feature = self.get_data(d, self.feature_from, None)
            # exposure = self.get_data(d, 'content.display_count', None)
            if feature:  # and exposure:
                # feature = int(feature / exposure * 10000) / 10000.0
                if type(feature) == list:
                    for i in feature:
                        if i not in feature_map:
                            feature_map[i] = 0
                        feature_map[i] += 1
                else:
                    if feature not in feature_map:
                        feature_map[feature] = 0
                    feature_map[feature] += 1
                hit += 1
        print("Done. Tot size: {tot} Hit cnt: {h_cnt}, distinct count: {d_cnt}".format(
            tot=tot, h_cnt=hit, d_cnt=len(feature_map)))
        if len(feature_map) <= 100:
            print(feature_map)

        if self.cut > 0:
            feature_list = [(i, j) for i, j in feature_map.items()]
            feature_list.sort(key=lambda x: x[0])
            tot = sum([i[1] for i in feature_list])
            single_cnt = tot / self.cut
            cnt = 0
            res = []
            for x, y in feature_list:
                cnt += y
                if cnt > single_cnt:
                    res += [x]
                    cnt = cnt % single_cnt
            print("Cut result: ", res)


if __name__ == "__main__":
    FLAGS(sys.argv)
    data_file_path = FLAGS.data_path
    feature_from = FLAGS.feature
    cut = FLAGS.cut
    ig_cutter = FeatureCount(data_file_path, feature_from, cut)
    ig_cutter.do_cnt()
