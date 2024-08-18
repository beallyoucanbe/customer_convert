case_dict = {

    # #同一个用户多次请求。
    "same_user_headline": {
        # "code": 200,
        "url": "curl -H \"Content-Type: application/json\" -X POST -d\'{{\"scene_id\":\"headline_articles\",\"recommend_bar\":\"hea_a\",\"log_id\":\"{log_id}\",\"uid\":\"{distinct_id}\",\"limit\":\"20\",\"exp_id\":\"sensors_rec\",\"enforce_exps\":\"baseline\"}}\' http://rec03:8201/api/rec/headline",
        "has_same": False,
        "number": 20,
    },
    # 不同新用户请求
    "new_user_headline": {
        "code": 200,
        "url": "curl -H \"Content-Type: application/json\" -X POST -d\'{{\"scene_id\":\"headline_articles\",\"recommend_bar\":\"hea_a\", \"log_id\":\"{log_id}\",\"uid\":\"{distinct_id}\",\"limit\":\"20\",\"exp_id\":\"sensors_rec\",\"semoniter\":\"1\",\"enforce_exps\":\"baseline\"}}\' http://rec03:8201/api/rec/headline",
        "number": 20,
        "retrieve_id": {
            "hot": {
                "method": "above",
                "number": 0
            },
        },
        "item_type": {
            "article": {
                "method": "above",
                "number": 0
            },
            "video": {
                "method": "above",
                "number": 0
            },
        },
        "timeliness": {
            "article": "22H"
        },
    },
    # 不同的 有非实时历史行为的老用户
    "old_user_headline": {
        "code": 200,
        "url": "curl -H \"Content-Type: application/json\" -X POST -d\'{{\"scene_id\":\"headline_articles\",\"recommend_bar\":\"hea_a\", \"log_id\":\"{log_id}\",\"uid\":\"{distinct_id}\",\"limit\":\"20\",\"exp_id\":\"sensors_rec\",\"semoniter\":\"1\",\"enforce_exps\":\"baseline\"}}\' http://rec03:8201/api/rec/headline",
        "number": 20,
        "retrieve_id": {
            "nlp_similar": {
                "method": "above",
                "number": 0
            },
        },
        "item_type": {
            "article": {
                "method": "above",
                "number": 0
            },
            "video": {
                "method": "above",
                "number": 0
            },
        },
        "timeliness": {
            "article": "22H"
        },
    },
    # 不同的 有实时历史行为的老用户
    "rt_old_user_headline": {
        "code": 200,
        "url": "curl -H \"Content-Type: application/json\" -X POST -d\'{{\"scene_id\":\"headline_articles\",\"recommend_bar\":\"hea_a\", \"log_id\":\"{log_id}\",\"uid\":\"{distinct_id}\",\"limit\":\"20\",\"exp_id\":\"sensors_rec\",\"semoniter\":\"1\",\"enforce_exps\":\"baseline\"}}\' http://rec03:8201/api/rec/headline",
        "number": 20,
        "retrieve_id": {
            "nlp_similar_rt": {
                "method": "above",
                "number": 0
            },
        },
        "item_type": {
            "article": {
                "method": "above",
                "number": 0
            },
            "video": {
                "method": "above",
                "number": 0
            },
        },
        "timeliness": {
            "article": "22H"
        },
    },

    # "same_user_channel": {
    #     #"code": 200,
    #     "url": "curl -H \"Content-Type: application/json\" -X POST -d\'{{\"scene_id\":\"channel_articles\",\"recommend_bar\":\"cha_a_article_yule\", \"log_id\":\"{log_id}\",\"uid\":\"{distinct_id}\",\"limit\":\"20\",\"exp_id\":\"sensors_rec\",\"channel\":\"yule\",\"enforce_exps\":\"baseline\"}}\' http://rec03:8201/api/rec/channel",
    #     "has_same": False,
    #     "number": 20,
    # },
    # "new_user_channel": {
    #     "code": 200,
    #     "url": "curl -H \"Content-Type: application/json\" -X POST -d\'{{\"scene_id\":\"channel_articles\",\"recommend_bar\":\"cha_a_article_yule\", \"log_id\":\"{log_id}\",\"uid\":\"{distinct_id}\",\"limit\":\"20\",\"exp_id\":\"sensors_rec\",\"channel\":\"yule\",\"semoniter\":\"1\",\"enforce_exps\":\"baseline\"}}\' http://rec03:8201/api/rec/channel",
    #     "number": 20,
    #     "retrieve_id": {
    #         "hot": {
    #             "method": "above",
    #             "number": 0
    #         },
    #     },
    #     "item_type": {
    #         "article": {
    #             "method": "above",
    #             "number": 0
    #         },
    #         "video": {
    #             "method": "above",
    #             "number": 0
    #         },
    #     },
    #     # "category": {
    #     #     "article_yule": {
    #     #         "method": "equal",
    #     #         "number": 20
    #     #     }
    #     # },
    #     #"timeliness": {
    #     #    "article": "22H"
    #     #},
    # },
    # "rt_old_user_channel": {
    #     "code": 200,
    #     "url": "curl -H \"Content-Type: application/json\" -X POST -d\'{{\"scene_id\":\"channel_articles\",\"recommend_bar\":\"cha_a_article_yule\", \"log_id\":\"{log_id}\",\"uid\":\"{distinct_id}\",\"limit\":\"20\",\"exp_id\":\"sensors_rec\",\"channel\":\"yule\",\"semoniter\":\"1\",\"enforce_exps\":\"baseline\"}}\' http://rec03:8201/api/rec/channel",
    #     "number": 20,
    #     "retrieve_id": {
    #         "nlp_similar_rt": {
    #             "method": "above",
    #             "number": 0
    #         },
    #     },
    #     "item_type": {
    #         "article": {
    #             "method": "above",
    #             "number": 0
    #         },
    #         "video": {
    #             "method": "above",
    #             "number": 0
    #         },
    #     },
    #     # "category": {
    #     #     "article_yule": {
    #     #         "method": "equal",
    #     #         "number": 20
    #     #     }
    #     # },
    #     #"timeliness": {
    #     #   "article": "22H"
    #     #},
    # },
    #
    # "same_user_channel_video": {
    #     #"code": 200,
    #     "url": "curl -H \"Content-Type: application/json\" -X POST -d\'{{\"scene_id\":\"channel_videos\",\"recommend_bar\":\"cha_v_video_redian\", \"log_id\":\"{log_id}\",\"uid\":\"{distinct_id}\",\"limit\":\"20\",\"exp_id\":\"sensors_rec\",\"category\":\"redian\",\"enforce_exps\":\"baseline\"}}\' http://rec03:8201/api/rec/channel_video",
    #     "has_same": False,
    #     "number": 20,
    # },
    # "new_user_channel_video": {
    #     "code": 200,
    #     "url": "curl -H \"Content-Type: application/json\" -X POST -d\'{{\"scene_id\":\"channel_videos\",\"recommend_bar\":\"cha_v_video_redian\", \"log_id\":\"{log_id}\",\"uid\":\"{distinct_id}\",\"limit\":\"20\",\"exp_id\":\"sensors_rec\",\"category\":\"redian\",\"semoniter\":\"1\",\"enforce_exps\":\"baseline\"}}\' http://rec03:8201/api/rec/channel_video",
    #     "number": 20,
    #     "retrieve_id": {
    #         "hot": {
    #             "method": "above",
    #             "number": 0
    #         },
    #     },
    #     "item_type": {
    #         "video": {
    #             "method": "equal",
    #             "number": 20
    #         },
    #     },
    #     "timeliness": {
    #         "video": "6D"
    #     },
    # },
    # "old_user_channel_video": {
    #     "code": 200,
    #     "url": "curl -H \"Content-Type: application/json\" -X POST -d\'{{\"scene_id\":\"channel_videos\",\"recommend_bar\":\"cha_v_video_redian\", \"log_id\":\"{log_id}\",\"uid\":\"{distinct_id}\",\"limit\":\"20\",\"exp_id\":\"sensors_rec\",\"category\":\"redian\",\"semoniter\":\"1\",\"enforce_exps\":\"baseline\"}}\' http://rec03:8201/api/rec/channel_video",
    #     "number": 20,
    #     "retrieve_id": {
    #         "hmf": {
    #             "method": "above",
    #             "number": 0
    #         },
    #     },
    #     "item_type": {
    #         "video": {
    #             "method": "equal",
    #             "number": 20
    #         },
    #     },
    #     "timeliness": {
    #         "video": "6D"
    #     },
    # },
    #
    # "relevents": {
    #     "code": 200,
    #     "url": "curl -H \"Content-Type: application/json\" -X POST -d\'{{\"scene_id\":\"relevent_articles\",\"recommend_bar\":\"rel_a\", \"log_id\":\"{log_id}\",\"uid\":\"semoniter\",\"item_id\":\"{item_id}\",\"limit\":\"20\",\"exp_id\":\"sensors_rec\",\"semoniter\":\"1\",\"enforce_exps\":\"baseline\"}}\' http://rec03:8201/api/rec/relevents",
    #     "number": 20,
    #     "item_type": {
    #         "article": {
    #             "method": "equal",
    #             "number": 20
    #         },
    #     },
    #     "timeliness": {
    #         "article": "16D"
    #     },
    # },
    #
    # "relevents_videos": {
    #     "code": 200,
    #     "url": "curl -H \"Content-Type: application/json\" -X POST -d\'{{\"scene_id\":\"relevant_videos\",\"recommend_bar\":\"rel_v\", \"log_id\":\"{log_id}\",\"uid\":\"semoniter\",\"item_id\":\"{item_id}\",\"limit\":\"20\",\"exp_id\":\"sensors_rec\",\"semoniter\":\"1\",\"enforce_exps\":\"baseline\"}}\' http://rec03:8201/api/rec/relevant_videos",
    #     "number": 20,
    #     "item_type": {
    #         "video": {
    #             "method": "equal",
    #             "number": 20
    #         },
    #     },
    #     "timeliness": {
    #         "video": "31D"
    #     },
    # }

}
