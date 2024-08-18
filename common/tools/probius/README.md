# Debug 工具  Probius

Probius 将构造模拟请求，发送给线上服务器，在返回时将会带上处理请求的几乎所有信息。

## 初始化

首先你需要填写一个 probius_conf.yaml 的配置文件，如下，你可以在 ~/sr/common/tools/probius 下找到 example

将填好的配置放入 /home/sa_cluster/sr/{company}/conf 下，并建出 /home/sa_cluster/sr/conf 下建出软链

或者将其放入一个不会被升级覆盖的地方，每次执行 probius 时手动指定配置位置

### 基本参数

可不填，可增加自己的 key value 参数对

参数对有两种，一是 k (str) -> v (str)，一是 k (str) -> v (list)

例如下面的 item_type，在执行 debug 请求构造时，会让你进行手动选择其中之一。

```yaml
# 基本参数，只有一个值的参数，会在执行中自动使用.
distinct_id: probius
item_type:
  - all_item_type
  - article
  - video
category:
  - all_category
  - yule
  - redian
limit: '20'
enforce_exps:
  - baseline
```

### 兼容性参数

可不填。

兼容性参数会替换生成的请求体的字段名称，例如下面的配置，将 distinct_id 替换为 uid，将 category 替换为 channel。

```yaml
# 兼容性参数，会替换最终请求中的字段
compat_args:
  distinct_id: uid
  category: channel
```

### nginx 参数

必填。

nginx 参数不一定需要是 nginx 地址，也可以是单个 web service 的地址

```yaml
# nginx 地址
nginx:
  - http://rec03:8201
```

### API 参数

必填。

列出该项目上常用的 API，方便后续选择。

```yaml
# API 地址参数
api:
  - /api/rec/channel
  - /api/rec/headline

```

### 取 item 信息的 sql

如果需要其余 item 信息，需要填

SQL 的前两列必须为 item_id 与 item_type，where 条件处的 concat(item_id, ',', item_type) 请勿改动

```yaml
# item 表信息，前两列必须为 item_id, item_type，留出一个 {} 用来填写条件
sql: /*SA(production)*/ select item_id, item_type, title, releasetime from items where concat(item_id, ',', item_type) in ({})
```

## 使用方法

执行 `python3 ~/sr/common/tools/probius/probius.py -c {conf_path} -o {output_path}` 即可，需要注意初始化中提到的关于配置文件位置的问题，如果配置放入了 /home/sa_cluster/sr/conf 之中（或建出了软链），则不需要使用 -c 进行配置位置的指定。

> 使用 **python3**（sp 的 python）进行执行，因为 sr_python 缺少 readline 库.

执行中需要选择配置里定义的各种参数，填写数字即可选择；若参数的值不存在于配置中，可直接输入非数字（空字符串），进入手动输入状态进行手动输入。

> 或者添加 `-q` 参数，使用快速模式，此模式下将全部使用配置中的第一个参数。
> 或者添加 `-i` 参数，直接填写 URL 和 JSON，会记录本机器的历史填写。
> 或者添加 `-ko` 参数，在命令行调用中直接指定使用第几个参数。ex. `-ko distinct_id=0 item_type=1`
> 或者添加 `-kv` 参数，在命令行调用中直接指定使用第几个参数。ex. `-kv distinct_id=abcd123 item_type=article`

若参数不在配置之中（或者自动使用的参数的值），可在最后的“输入其它参数”阶段进行参数填写。

本工具的中间提示完整，若有不明可评论指出。
