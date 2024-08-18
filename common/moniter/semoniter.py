import json
import uuid
import collections
from datetime import datetime, timedelta


class Semoniter(object):
    def __init__(self, semoniter_conf, case_conf):
        """
        :param semoniter_conf: 读取的各家的语义监控conf
        case_dict = {
    "case1": {
        "url": "curl -H \"Content-Type: application/json\" -X POST -d\'{\"scene_id\":\"detailrelatedRecommend\", \"log_id\":\"semoniter\",\"distinct_id\":\"{distinct_id}\",\"item_id\":\"2403486\",\"limit\":\"10\",\"exp_id\":\"sensors_rec\",\"category\":\"general\",\"enforce_exps\":\"baseline\",\"semoniter\":\"1\"}\' http://rec01.xinshang.com:8201/api/rec/relevant",
        "has_same": False,
        "number": 10,
        "retrieve_id": {
            "hmf_item_similar": {
                "method": "number",
                "number": 1
            },
        },
        "item_type": {
            "article": {
                "method": "equal",
                "number": 1
            },
        },
    }
    }
        :param case_conf: 各种case的conf (distinct_id，item_id是啥之类的)
        """
        self.case_list = []
        self.max_example = 3
        cnt = 0
        for case in semoniter_conf:
            for example in case_conf[case]:
                self.case_list.append(
                    (case, semoniter_conf[case]['url'].format(distinct_id=example.get('distinct_id'),
                                                              item_id=example.get('item_id'),
                                                              item_type=example.get('item_type'),
                                                              log_id='semoniter_' + str(uuid.uuid4())
                                                              )))
                cnt += 1
                if cnt == self.max_example:
                    break
        # 初始化统计字典
        self.statistic_dict = {case: [] for case in semoniter_conf}

        self.case_condition_dict = semoniter_conf

    def set_case_return(self, case_name, case_url, json_body):
        _dict = {}
        raw_dict = json.loads(json_body)
        code = raw_dict['code']
        retrieve_id_dict = collections.defaultdict(int)
        item_type_dict = collections.defaultdict(int)
        category_dict = collections.defaultdict(int)
        item_id_list = []
        releasetime_list = []
        for item_dict in raw_dict.get('data', []):
            retrieve_id_dict[item_dict.get('retrieve_id')] += 1
            item_type_dict[item_dict.get('item_type')] += 1
            if item_dict.get('channel'):
                for category in item_dict['channel'].split(','):
                    category_dict[category] += 1
                item_id_list.append(
                    item_dict.get('item_id') + '|' + item_dict.get('item_type') + '|' + item_dict.get('channel'))

            elif item_dict.get('category'):
                for category in item_dict['category'].split(','):
                    category_dict[category] += 1
                item_id_list.append(
                    item_dict.get('item_id') + '|' + item_dict.get('item_type') + '|' + item_dict.get('category'))
            else:
                item_id_list.append(item_dict.get('item_id') + '|' + item_dict.get('item_type') + '|' + '')
            d = datetime.fromtimestamp(item_dict.get('releasetime', 0)).strftime("%Y-%m-%d %H:%M:%S")
            releasetime_list.append(d)
        item_number = len(raw_dict.get('data', []))
        _dict['url'] = case_url
        _dict['code'] = code
        _dict['retrieve_id_dict'] = retrieve_id_dict
        _dict['item_type_dict'] = item_type_dict
        _dict['category_dict'] = category_dict
        # todo 加上category_dict
        _dict['item_msg'] = dict(zip(item_id_list, releasetime_list))
        _dict['item_number'] = item_number
        _dict['execute_step_info'] = raw_dict.get('execute_step_info', {})
        if case_name in self.statistic_dict:
            self.statistic_dict[case_name].append(_dict)
        else:
            self.statistic_dict[case_name] = [_dict]

    # 断言
    def verify_code(self, case_name):
        """
        必须code全为200
        :param case_name:
        :return:
        """
        tag = 0
        final_dict = {}
        for request in self.statistic_dict[case_name]:
            if request['code'] != self.case_condition_dict[case_name]["code"]:
                tag = 1
                if final_dict:
                    final_dict[case_name + ':' + 'verify_code_failed'].append(request)
                else:
                    final_dict.update({case_name + ':' + 'verify_code_failed': [request]})
        if tag == 0:
            return True, {}
        return False, final_dict

    @staticmethod
    def verify_request(raw_dict, key, condition):
        if condition['method'] == 'equal' and raw_dict[key] == \
                condition['number']:
            return True
        elif condition['method'] == 'above' and raw_dict[key] > \
                condition['number']:
            return True
        elif condition['method'] == 'under' and raw_dict[key] < \
                condition['number']:
            return True
        return False

    def verify_retrieve_id(self, case_name):
        """
        retrieve id有一个case符合就算符合
        :param case_name:
        :return:
        """
        for request in self.statistic_dict[case_name]:
            tag = 0
            for retrieve_id in self.case_condition_dict[case_name]['retrieve_id']:
                if not self.verify_request(request['retrieve_id_dict'], retrieve_id,
                                           self.case_condition_dict[case_name]['retrieve_id'][retrieve_id]):
                    tag = 1
            if tag == 0:
                return True, {}

        return False, {case_name + ':' + 'verify_retrieve_id_failed': self.statistic_dict[case_name]}

    def verify_item_type(self, case_name):
        """
        必须全部符合
        :param case_name:
        :return:
        """
        tag = 0
        final_dict = {}
        for request in self.statistic_dict[case_name]:
            for item_type in self.case_condition_dict[case_name]['item_type']:
                if not self.verify_request(request['item_type_dict'], item_type,
                                           self.case_condition_dict[case_name]['item_type'][item_type]):
                    tag = 1
                    if final_dict:
                        final_dict[case_name + ':' + 'verify_item_type_failed'].append(request)
                    else:
                        final_dict.update({case_name + ':' + 'verify_item_type_failed': [request]})
        if tag == 0:
            return True, {}
        return False, final_dict

    def verify_number(self, case_name):
        """
        必须全部符合
        :param case_name:
        :return:
        """
        tag = 0
        final_dict = {}
        for request in self.statistic_dict[case_name]:
            if request['item_number'] != self.case_condition_dict[case_name]['number']:
                tag = 1
                if final_dict:
                    final_dict[case_name + ':' + 'verify_item_number_failed'].append(request)
                else:
                    final_dict.update({case_name + ':' + 'verify_item_number_failed': [request]})
        if tag == 0:
            return True, {}
        return False, final_dict

    def verify_category(self, case_name):
        """
        必须全部符合
        :param case_name:
        :return:
        """
        tag = 0
        final_dict = {}
        for request in self.statistic_dict[case_name]:
            for category in self.case_condition_dict[case_name]['category']:
                if not self.verify_request(request['category_dict'], category,
                                           self.case_condition_dict[case_name]['category'][category]):
                    tag = 1
                    if final_dict:
                        final_dict[case_name + ':' + 'verify_category_failed'].append(request)
                    else:
                        final_dict.update({case_name + ':' + 'verify_category_failed': [request]})
        if tag == 0:
            return True, {}
        return False, final_dict

    # def verify_channel(self, case_name):
    #     # todo 等服务改完再测吧
    #     tag = 0
    #     for request in self.statistic_dict[case_name]:
    #         for channel in self.case_condition_dict[case_name]['channel']:
    #             if not self.verify_request(request['channel_dict'], channel,
    #                                        self.case_condition_dict[case_name]['channel'][channel]):
    #                 tag = 1
    #     if tag == 0:
    #         return True, {}
    #     return False, {case_name + ':' + 'verify_channel_failed': self.statistic_dict[case_name]}

    def verify_has_same(self, case_name):
        if self.case_condition_dict[case_name]["has_same"] is False:
            item_list = []
            for request in self.statistic_dict[case_name]:
                item_list.extend(list(request['item_msg'].keys()))
            if len(item_list) == len(set(item_list)):
                return True, {}
        else:
            return True, {}
        return False, {case_name + ':' + 'verify_has_same_failed': self.statistic_dict[case_name]}

    def verify_timeliness(self, case_name):
        timestamp = str(datetime.now())
        final_dict = {}
        tag = 0
        for type_category in self.case_condition_dict[case_name]['timeliness']:
            if "D" in self.case_condition_dict[case_name]["timeliness"][type_category]:
                timestamp = str(
                    datetime.now() - timedelta(
                        days=int(self.case_condition_dict[case_name]["timeliness"][type_category][:-1])))
            elif "H" in self.case_condition_dict[case_name]["timeliness"][type_category]:
                timestamp = str(
                    datetime.now() - timedelta(
                        hours=int(self.case_condition_dict[case_name]["timeliness"][type_category][:-1])))

            if len(type_category.split('|')) == 1:
                type = type_category
                for request in self.statistic_dict[case_name]:
                    for item_id, releasetime in request['item_msg'].items():
                        if releasetime < timestamp and releasetime != '1970-01-01 08:00:00' and \
                                item_id.split('|')[1] == type:
                            tag = 1
                            if final_dict:
                                final_dict[case_name + ':' + 'verify_timeliness_failed'].append(request)
                            else:
                                final_dict.update({case_name + ':' + 'verify_timeliness_failed': [request]})
            elif len(type_category.split('|')) == 2:
                for request in self.statistic_dict[case_name]:
                    for item_id, releasetime in request['item_msg'].items:
                        if releasetime < timestamp and releasetime != '1970-01-01 08:00:00':
                            if item_id.split('|')[1] == type_category.split('|')[0] and \
                                    item_id.split('|')[2] in type_category.split('|')[1]:
                                tag = 1
                                if final_dict:
                                    final_dict[case_name + ':' + 'verify_timeliness_failed'].append(request)
                                else:
                                    final_dict.update({case_name + ':' + 'verify_timeliness_failed': [request]})
        if tag == 0:
            return True, {}
        else:
            return False, final_dict

    def verify(self):
        final_dict = {}
        for case in self.statistic_dict:
            for step in self.case_condition_dict[case]:
                if hasattr(self, "verify_" + step):
                    tag, message = getattr(self, "verify_" + step)(case)
                    if tag is False:
                        final_dict.update(message)
        # 同一个case如果多次出问题就算真的有问题
        for key in final_dict:
            if len(key) <= 1:
                final_dict.pop(key)
        if final_dict:
            return False, final_dict
        else:
            return True, {}
