# coding: utf-8

import os
import sys
import json
import logging
import requests
import argparse
from importlib.machinery import SourceFileLoader

# import base version
sys.path.append(os.path.join(os.path.abspath(os.path.dirname(__file__)), '../../web_service/'))
# common path
sys.path.append(os.path.join(os.path.abspath(os.path.dirname(__file__)), '../../../'))
from common.web_service.palantir.exp import ExpDistributor, process_scene_map_info
from common.web_service.palantir.app import set_env
from common.web_service.palantir.app import GLOBAL_CONF
from hyperion_client.config_manager import ConfigManager
cm = ConfigManager()


class SenceMapParser(object):
    def __init__(self, config):
        self.project = getattr(config, 'project', '')
        self.username = getattr(config, 'username', '')
        self.password = getattr(config, 'password', '')
        self.srm_host = getattr(config, 'srm_host', '')
        self.token = cm.get_global_conf().get("super_api_token")
        self.default_recommend_position = 'default'
        self.default_recommend_type = 'PERSONAL'
        self.default_flow_rate = 100

        self.recommend_project = GLOBAL_CONF.get('project_name')

        self._login_header = self._login()

    def _login(self):
        return {'Content-Type': 'application/json'}

    def new_scene(self, scene_name):
        scene_info = {'name': scene_name,
                      'cname': scene_name,
                      'recommend_position': self.default_recommend_position,
                      'recommend_type': self.default_recommend_type,
                      'sr_project': self.project}
        url = 'http://{host}:8107/api/v2/srm/recommend_scenes?token={token}&project={project}&recommend_project={recommend_project}'.format(
            host=self.srm_host,
            token=self.token,
            project=self.project,
            recommend_project=self.recommend_project,
        )
        logging.info('new_scene url: %s' % url)
        r = requests.put(url, headers=self._login_header, data=json.dumps(scene_info))
        logging.info('add new scene res code: %s' % r.status_code)

        if r.status_code == 200:
            logging.info('Add new scene: %s Success.' % scene_name)
            return r.json()
        else:
            logging.info("Add new scene: %s Failed. status_code %s, text: %s" % (scene_name, r.status_code, r.text))
            raise ValueError("Add new scene: %s Failed." % scene_name)

    def get_scenes(self):
        # [{'cname': '首页推荐',
        #   'id': 1,
        #   'name': 'main_page',
        #   'recommend_position': '首页推荐',
        #   "recommend_type": {
        #         "name": "video_personal",  # 推荐场景名字
        #         "cname": "视频个性化",  # 中文名
        #         "base_type": "PERSONAL",  # 推荐场景大类。RELEVANT 相关 HOT 热门 PERSONAL 个性化
        #     },
        #   'sr_project': 'xiongmao'}]
        url = 'http://{host}:8107/api/v2/srm/recommend_scenes?token={token}&project={project}&recommend_project={recommend_project}'.format(
            host=self.srm_host,
            token=self.token,
            project=self.project,
            recommend_project=self.recommend_project,
        )
        logging.info("get_scenes url: %s" % url)
        r = requests.get(url, headers=self._login_header)
        logging.info("r status: %s " % r.status_code)
        return r.json()

    def delete_scene(self, scene_name):
        scene_id = None
        scenes = self.get_scenes()
        for scene in scenes:
            if scene['name'] == scene_name:
                scene_id = scene['id']
        if scene_id is None:
            logging.warning('No such scene: %s, No need to delete.' % scene_name)
            return

        url = 'http://{host}:8107/api/v2/srm/recommend_scenes/{scene_id}?token={token}&project={project}&recommend_project={recommend_project}'.format(
            host=self.srm_host,
            scene_id=scene_id,
            token=self.token,
            project=self.project,
            recommend_project=self.recommend_project,
        )
        logging.info("delete_scene url: %s" % url)
        r = requests.delete(url, headers=self._login_header)
        if r.status_code == 200:
            logging.info('Delete scene Success: %s' % scene_name)
        return

    def new_map(self, scene_name, exp_name, traffic_size, map_info):
        scene_id = None
        scenes = self.get_scenes()
        logging.info("scenes: %s" % scenes)
        logging.info("scene_name: %s" % scene_name)

        project_name = GLOBAL_CONF.get("project_name")
        conf = GLOBAL_CONF.get_project_config(project_name)
        section_conf = conf.section_config.section_conf

        for scene in scenes:
            logging.info("scene is %s" % scene)

            # 栏位动态生成的 scene， 取 base_scene
            if scene_name in section_conf:
                scene_name = section_conf[scene_name]['base']

            if scene['name'] == scene_name:
                scene_id = scene['id']
        if scene_id is None:
            # 新建scene
            logging.warning('No such scene: %s, automatic add scene: %s' % (scene_name, scene_name))
            res = self.new_scene(scene_name)
            scene_id = res['id']

        scene_map_info = {
            "summary": {"name": exp_name,
                        "cname": exp_name,
                        "flow_rate": str(traffic_size)},
            "context": json.dumps(map_info),
        }

        url = 'http://{host}:8107/api/v2/srm/recommend_scenes/{scene_id}/maps?token={token}&project={project}&recommend_project={recommend_project}'.format(
            host=self.srm_host,
            scene_id=scene_id,
            token=self.token,
            project=self.project,
            recommend_project=self.recommend_project,
        )
        logging.info("new_map url: %s" % url)
        r = requests.put(url, headers=self._login_header,
                         data=json.dumps(scene_map_info))
        if r.status_code == 200:
            logging.info("Add new scene map success: %s." % exp_name)
            return r.json()
        else:
            logging.info('Add new scene map failed: %s. status_code %s, text: %s' % (exp_name, r.status_code, r.text))
        # {'context': '...省略无数...',
        #  'summary': {'cname': '增加nlp召回实验',
        #              'flow_rate': 50,
        #              'id': 11,
        #              'name': 'nlp_test'}}


def entrance():
    parser = argparse.ArgumentParser(description="Sensors Recommender Scene Map Parser")
    # 确认是否只需要scene_config.py就好
    parser.add_argument('-c', '--conf', required=True, help="smp_conf")
    parser.add_argument('--web_service_conf', required=True, help="web service conf path")
    # parser.add_argument('-a', '--action', )
    args = parser.parse_args()

    # 初始化环境
    set_env(args.web_service_conf)
    project_name = GLOBAL_CONF.get("project_name")
    web_service_conf = GLOBAL_CONF.get_project_config(project_name)

    scene_conf = web_service_conf.scene_config.scene_conf
    for scene_id, conf in scene_conf.items():
        # 默认实验，用来生成运行图，实际逻辑不会用到
        if not conf['traffic'] or set(conf['traffic']) == {'default_domain', 'default_layer'}:
            demo_exp_name = '__exp_demo__'
            conf['exp'][demo_exp_name] = {
                'rank': {
                    'name': 'base'
                }
            }
            conf['traffic'] = {
                'diversion': 'distinct_id',
                'l1': {
                    'name': 'l1',
                    'type': 'layer',
                    'layer_id': 1,
                    'diversion': 'distinct_id',
                    'current_domain': 'default_domain',
                    'previous_layer': None
                },
                demo_exp_name: {
                    'name': demo_exp_name,
                    'type': 'experiment',
                    'exp_tag': demo_exp_name,
                    'traffic_size': 0,
                    'current_layer': 'l1'},
            }

    smp_conf = SourceFileLoader("smp_conf", os.path.abspath(args.conf)).load_module()
    ed = ExpDistributor(scene_conf=scene_conf,
                        white_list_conf=getattr(web_service_conf.exp_white_list_config, "exp_white_lists", {}))
    scene_map_list = ed.generate_conf_export_rich()
    scene_map_res = process_scene_map_info(scene_map_list)

    smp = SenceMapParser(smp_conf)
    # 这里不删除场景，一定要保证scene_conf.py里的scene_id和srm后台流程里的新建场景的场景英文名相同。
    # for scene_name in set([x[0] for x in scene_map_res]):
    #     smp.delete_scene(scene_name)
    for scene_name, exp_name, traffic_size, map_info in scene_map_res:
        smp.new_map(scene_name, exp_name, traffic_size, map_info)


if __name__ == "__main__":
    entrance()
