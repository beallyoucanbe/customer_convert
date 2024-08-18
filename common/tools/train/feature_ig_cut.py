# coding:utf-8

# 计算连续特征的信息增益

import sys
import math
import json
from absl import flags

FLAGS = flags.FLAGS
flags.DEFINE_string('data_path', None, 'data file path.')
flags.DEFINE_string('feature', None, 'feature')
flags.DEFINE_string('label', None, 'label')
flags.DEFINE_integer('cut_limit', 10000, 'max cut times')
flags.DEFINE_float('min_cut_percent', 1, 'minimal cut percent(0~1)')


class FeatureIGCut():
    def __init__(self, file_path, feature_from, label_from):
        with open(file_path) as f:
            self.data = f.readlines()
            f.close()
        self.feature_from = feature_from
        self.label_from = label_from

    def get_data(self, data, get_from, default_value):
        idx = get_from.find('.')
        now = get_from[:idx] if idx >= 0 else get_from
        rest = get_from[idx + 1:] if idx >= 0 else ''
        if now not in data:
            return default_value
        return self.get_data(data[now], rest, default_value) if rest else data[now]

    def calc_entropy(self, data):
        n = len(data)
        label_map = {}
        for i in data:
            label_map[i[1]] = label_map[i[1]] + 1 if i[1] in label_map else 1

        e = 0.0
        for i in label_map.values():
            p = float(i) / n
            e += p * math.log(p, 2)
        return -e

    def calc_IG(self, data, cut_point):
        res = self.calc_entropy(data)
        start = 0
        n = len(data)
        for i in cut_point:
            e_single = self.calc_entropy(data[start:i + 1])
            cnt = i + 1 - start
            res -= e_single * cnt / n
            start = i
        e_single = self.calc_entropy(data[start:])
        cnt = n - start
        res -= e_single * cnt / n
        return res

    def check_range(self, check_points, step):
        check_points.sort()
        for i in range(len(check_points) - 1):
            if check_points[i + 1] - check_points[i] < step:
                return False
        return True

    def find_check_points(self, n, cut_points, step):
        res = []
        check_points = cut_points + [0, n - 1]

        for check_point in check_points:
            # forward
            point = check_point + step
            while point < n:
                if self.check_range(check_points + [point], step):
                    res += [point]
                else:
                    break
                point += step

            # backward
            point = check_point - step
            while point > 0:
                if self.check_range(check_points + [point], step):
                    res += [point]
                else:
                    break
                point -= step
        return res

    def do_cut(self, cut_limit, min_cut_percent):
        # get data
        data = []
        tot = len(self.data)
        hit = 0
        for d in self.data:
            d = json.loads(d)
            feature = self.get_data(d, self.feature_from, None)
            label = self.get_data(d, self.label_from, 0)
            if feature and feature >= 0:
                data.append((float(feature), float(label)))
                hit += 1
        # sort data by feature
        data.sort()
        print("Read data done. Hit cnt: {hit_cnt}, Feature coverage: {f_cov}".format(
            hit_cnt=hit, f_cov=hit / tot))

        # calculate cut points
        n = len(data)
        step = max(int(n * min_cut_percent), 1)
        cut_points = []
        old_IG = 0
        for i in range(cut_limit):
            print("Step {i} start...".format(i=i))
            max_IG = 0.0
            check_points = self.find_check_points(n, cut_points, step)
            for cut_idx in check_points:
                new_points = cut_points + [cut_idx]
                new_points.sort()
                new_IG = self.calc_IG(data, new_points)
                if new_IG > max_IG:
                    max_IG = new_IG
                    res = cut_idx

            delta_IG = max_IG - old_IG
            if delta_IG > 0:
                cut_points += [res]
                cut_points.sort()
                old_IG = max_IG
                print("Step {i}: IG={IG}".format(i=i, IG=delta_IG))
                print(cut_points)
                res = []
                for i in cut_points:
                    res += [data[i][0]]
                print(res)
            else:
                break

        res = []
        for i in cut_points:
            res += [data[i][0]]
        return res


if __name__ == "__main__":
    FLAGS(sys.argv)
    data_file_path = FLAGS.data_path
    feature_from = FLAGS.feature
    label_from = FLAGS.label
    cut_limit = FLAGS.cut_limit
    min_cut_percent = FLAGS.min_cut_percent

    ig_cutter = FeatureIGCut(data_file_path, feature_from, label_from)
    res = ig_cutter.do_cut(cut_limit, min_cut_percent)
    print("cnt: ", len(res))
    print(res)
