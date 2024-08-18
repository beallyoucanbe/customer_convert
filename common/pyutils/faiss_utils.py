import glob
import math
import os
import re

import faiss
import numpy as np
from scipy.sparse.compressed import _cs_matrix


def build_faiss_index(vectors, can_indices=None, distance="L2", nprobe=5, force=True, use_index_factory=False):
    """建立 Faiss 搜索索引.

    注意，faiss 自带 IDMap，此处建出的搜索索引，搜索结果会以其在 vectors 里的 index 作为返回的 index.

    Args:
        vectors (matrix): 所有向量的集合，可以是 np.array 或 scipy 的稀疏矩阵，类型必须是 np.float32.
        can_indices (list(int)): 该组搜索索引中要包含的向量，其在 vectors 里的 index. 若为 None 表示为
            全部 vectors 建立搜索索引.
        distance (str): 搜索索引使用的距离. 现在支持: L2 (搜索 L2 距离最短), COS (搜索 cosine similarity),
            INNER_PRODUCT (搜索 maximum inner product).
        nprobe (int): faiss 使用 IVF 建索引时，搜索时使用的聚类数，越多结果越精确，对应速度越慢.
        force (bool): 是否强制建立 faiss 索引，如果为 False，则只会在使用了 IVF 的情况下，建立索引，否则
            强制建出索引
        use_index_factory(bool): 是否使用index_factory, 默认为False, 全量检索。 如果为True, 使用
            IVF进行检索

    Returns:
        faiss 的 index，用于 index.search.

    """
    if distance not in ("L2", "COS", "INNER_PRODUCT"):
        raise ValueError(f"不支持的 distance 类型: {distance}.")

    if can_indices is None:
        matrix = vectors
    else:
        matrix = vectors[can_indices]

    index_size, index_d = matrix.shape

    metric_type = None
    if distance in ("COS", "INNER_PRODUCT"):
        metric_type = faiss.METRIC_INNER_PRODUCT
    elif distance in ("L2",):
        metric_type = faiss.METRIC_L2

    if metric_type is None:
        raise ValueError("无效的 metric type.")

    if use_index_factory:
        index_factory_args = list()
        if can_indices is not None:
            index_factory_args.append("IDMap")

        # 参考此处进行 cosine similarity 搜索的方法: https://github.com/facebookresearch/faiss/wiki/MetricType-and-distances
        if distance in ("COS",):
            index_factory_args.append("L2norm")

        is_ivf = False
        # 参见此处 IVF 聚类个数公式: https://github.com/facebookresearch/faiss/wiki/Guidelines-to-choose-an-index
        IVF_x = 4 * math.sqrt(index_size)
        if 36 * IVF_x < index_size:
            index_factory_args.append(f"IVF{int(IVF_x)}")
            is_ivf = True

        if not is_ivf and not force:
            return
        # 参考 faiss 的源码：https://github.com/facebookresearch/faiss/blob/master/faiss/index_factory.cpp
        index_factory_args.append("Flat")
        index = faiss.index_factory(index_d, ",".join(index_factory_args), metric_type)
    else:
        quantizer = faiss.IndexFlatL2(index_d)
        index = faiss.IndexIVFFlat(quantizer, index_d, nprobe, faiss.METRIC_INNER_PRODUCT)

    # 将稀疏矩阵转换为 numpy array
    if isinstance(matrix, _cs_matrix):
        matrix = matrix.toarray()

    # faiss 必须使用 float32 类型的向量
    matrix = matrix.astype(np.float32)

    if not index.is_trained:
        index.train(matrix)

    if can_indices is not None:
        index.add_with_ids(matrix, np.array(can_indices))
    else:
        index.add(matrix)
    index.nprobe = nprobe
    return index


def load_can_index_file(index_path):
    """读取所有 can_i 文件的函数.

    Args:
        index_path (str): can_i 文件所在的目录.

    Yields:
        (k, v): k 为 can_i 文件组名，比如 all_item_type:all_category.
            v 为 list(int)，该组下所有的 index.

    """
    pattern = re.compile(r"can_i_(.+)\.csv\.indices")
    for index_file in glob.glob(os.path.join(index_path, "can_i_*.csv.indices")):
        name = pattern.fullmatch(os.path.split(index_file)[1]).group(1)
        indices = list()
        with open(index_file, "rb") as f:
            for line in f:
                _id = int(line.strip())
                indices.append(_id)
        yield (name, indices)


def load_faiss_index(index_path):
    """读取所有 .findex 后缀的 faiss 索引文件.

    Args:
        index_path (str): faiss 索引文件所在的目录.

    Yields:
        (k, v): k 为 index 名，比如 all_item_type:all_category.
            v 为 faiss model.

    """
    for index_file in glob.glob(os.path.join(index_path, "*.findex")):
        index = os.path.splitext(os.path.split(index_file)[1])[0]
        yield (index, faiss.read_index(index_file))
