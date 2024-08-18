# coding: utf-8

def standard_redis_key_wrapper(key, project_name):
    """ redis key 生成规则统一处理， 补上项目前缀

    :param key:
    :param project_name:
    :return:
    """
    # 2.8 不提供多租户功能，废掉前缀区分
    return key

    prefix = f"sr_saas:{project_name}"
    if not key.startswith(prefix):
        key = f"{prefix}:{key}"
    return key
