# coding:utf-8

# 计算连续特征的信息增益

import os
import sys
import json
import numpy as np
from absl import flags
import tensorflow as tf
from copy import deepcopy
from tensorflow import keras

FLAGS = flags.FLAGS
flags.DEFINE_string('data_path', None, 'data file path.')
flags.DEFINE_string('model_path', None, 'model path')


class MeanPool(keras.layers.Layer):
    def __init__(self, axis, **kwargs):
        self.supports_masking = True
        self.axis = axis
        super(MeanPool, self).__init__(**kwargs)

    def get_config(self):
        config = {
            'axis': self.axis
        }
        base_config = super(MeanPool, self).get_config()
        return dict(list(base_config.items()) + list(config.items()))

    def compute_mask(self, input, input_mask=None):
        # need not to pass the mask to next layers
        return None

    def call(self, x, mask=None):
        if mask is not None:
            if keras.backend.ndim(x) != keras.backend.ndim(mask):
                mask = keras.backend.repeat(mask, x.shape[-1])
                mask = tf.transpose(mask, [0, 2, 1])
            mask = keras.backend.cast(mask, keras.backend.floatx())
            x = x * mask
            return keras.backend.sum(x, axis=self.axis) / keras.backend.clip(keras.backend.sum(mask, axis=self.axis),
                                                                             1.0, 10000.0)
        else:
            return keras.backend.mean(x, axis=self.axis)

    def compute_output_shape(self, input_shape):
        output_shape = []
        for i in range(len(input_shape)):
            if i != self.axis:
                output_shape.append(input_shape[i])
        return tuple(output_shape)


class SumLayer(keras.layers.Layer):
    def __init__(self, axis, **kwargs):
        self.supports_masking = True
        self.axis = axis
        super(SumLayer, self).__init__(**kwargs)

    def get_config(self):
        config = {
            'axis': self.axis
        }
        base_config = super(SumLayer, self).get_config()
        return dict(list(base_config.items()) + list(config.items()))

    def compute_mask(self, input, input_mask=None):
        # do not pass the mask to the next layers
        return None

    def call(self, x, mask=None):
        if mask is not None:
            # mask (batch, time)
            mask = keras.backend.cast(mask, keras.backend.floatx())
            if keras.backend.ndim(x) != keras.backend.ndim(mask):
                mask = keras.backend.repeat(mask, x.shape[-1])
                mask = tf.transpose(mask, [0, 2, 1])
            x = x * mask
        if keras.backend.ndim(x) == 2:
            x = keras.backend.expand_dims(x)
        return keras.backend.sum(x, axis=self.axis)

    def compute_output_shape(self, input_shape):
        output_shape = []
        for i in range(len(input_shape)):
            if i != self.axis:
                output_shape.append(input_shape[i])
        if len(output_shape) == 1:
            output_shape.append(1)
        return tuple(output_shape)


class FeatureImportance:
    def __init__(self):
        pass

    def model_load(self, path):
        model = keras.models.load_model(path, custom_objects={'MeanPool': MeanPool, 'SumLayer': SumLayer})
        model.compile(optimizer=tf.train.AdamOptimizer(0.001),
                      loss='binary_crossentropy',
                      metrics=['accuracy'])
        print(model.summary())
        return model

    def load_file(self, filename):
        print("Load ", filename, " data start...")
        if not os.path.isfile(filename):
            print("Interaction file {} does not exists!".format(filename))
            exit(1)
        with open(filename) as f:
            data = f.readlines()
            f.close()
        print("Load ", filename, " data done. Size: ", len(data))
        return data

    def gen_train_data(self, train_data_path):
        train_data = self.load_file(train_data_path)

        features_dict = {}
        for data in train_data:
            data = json.loads(data)
            for (k, v) in data.items():
                if k not in features_dict:
                    features_dict[k] = []
                features_dict[k].append(v)
        for (k, v) in features_dict.items():
            features_dict[k] = np.array(v)

        labels = features_dict['label']
        del features_dict['label']

        return features_dict, labels

    @staticmethod
    def get_tf_session():
        init_op = tf.compat.v1.global_variables_initializer()
        local_init_op = tf.compat.v1.local_variables_initializer()
        sess = tf.compat.v1.Session()
        sess.run(init_op)
        sess.run(local_init_op)
        return sess


if __name__ == "__main__":
    FLAGS(sys.argv)
    data_file_path = FLAGS.data_path
    model_path = FLAGS.model_path

    fi = FeatureImportance()
    model = fi.model_load(model_path)
    test_input_dict, test_labels = fi.gen_train_data(data_file_path)

    # calculate original auc
    predict_result = model.predict(test_input_dict, batch_size=500)
    predict_tensor = tf.convert_to_tensor(predict_result)
    label_tensor = tf.convert_to_tensor(test_labels.tolist())
    auc_value, auc_op = tf.compat.v1.metrics.auc(label_tensor, predict_tensor)
    sess = fi.get_tf_session()
    sess.run(auc_op)
    org_auc = sess.run(auc_value)
    print("orginal auc: ", org_auc)

    res = {}
    for (k, v) in test_input_dict.items():
        test_data = deepcopy(test_input_dict)
        test_data[k][:] = 0

        predict_result = model.predict(test_data, batch_size=500)
        predict_tensor = tf.convert_to_tensor(predict_result)
        auc_value, auc_op = tf.compat.v1.metrics.auc(label_tensor, predict_tensor)
        sess = fi.get_tf_session()
        sess.run(auc_op)
        auc = sess.run(auc_value)
        print("auc of remove {}: {}, delta-auc: {}".format(k, auc, auc - org_auc))
        res[k] = auc - org_auc

    print("result: ", res)
