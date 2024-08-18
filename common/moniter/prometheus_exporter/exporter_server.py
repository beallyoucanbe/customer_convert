import prometheus_client
from prometheus_client import Gauge
from prometheus_client.core import CollectorRegistry
from flask import Response, Flask
import json

app = Flask(__name__)
registry = CollectorRegistry(auto_describe=False)
g_sr = Gauge("sr", "SR modules status", ['service', 'ip', 'host_name', 'method', 'event', 'group', 'product_line'], registry=registry)
redis_maxmemory = Gauge("redis_maxmemory", "biz_redis_memory_info",
                        ['service', 'ip', 'host_name', 'event', 'group', 'product_line'], registry=registry)
redis_usedmemory = Gauge("redis_usedmemory", "biz_redis_memory_info",
                         ['service', 'ip', 'host_name', 'event', 'group', 'product_line'], registry=registry)

message_file = '/home/sa_cluster/sr/moniter/prometheus_exporter/export_messages'


@app.route("/sensors_recommender")
def redis_status():
    with open(message_file, 'r') as f:
        message_list = json.load(f)
        for message in message_list:
            module_name = message['service']
            ip = message['ip']
            host_name = message['host_name']
            method = message['method']
            event = message['event']
            if method == 'maxmemory':
                redis_maxmemory.labels(module_name, ip, host_name, host_name + '_' + module_name, 'sr', 'sr').set(message['value'])
            elif method == 'used_memory':
                redis_usedmemory.labels(module_name, ip, host_name, host_name + '_' + module_name, 'sr', 'sr').set(message['value'])
            g_sr.labels(module_name, ip, host_name, method, event, 'sr', 'sr').set(message['value'])
    return Response(prometheus_client.generate_latest(registry),
                    mimetype="text/plain")


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=9099)
