# /tmp/bkjob 中文件是否可以删除

## 现象描述

目标主机 `/tmp/bkjob` 目录下的文件是否可以删除？

## 问题定位及处理

1. `/tmp/bkjob/user00` 这个目录里面的脚本，一般都是用户自己的历史执行脚本，用户可以自行删除。
2. 自动清理策略可查看 agent 配置文件 `/usr/local/gse2/agent/etc/agent.conf`，其中/usr/local/gse2为GSE Agent根目录，不同环境可能不一样，可通过ps -ef|grep gse查看实际值。
